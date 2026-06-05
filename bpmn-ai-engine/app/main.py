from __future__ import annotations

import json
import logging
import re
from difflib import SequenceMatcher
from typing import Any, Dict, List

from fastapi import FastAPI, HTTPException, Request
from fastapi.middleware.cors import CORSMiddleware
from openai import OpenAI
from pydantic import BaseModel, Field

from .agent_service import DiagramAgentService
from .config import get_settings
from .models import AgentRequest, AgentResult, HealthResponse

settings = get_settings()
logging.basicConfig(level=settings.log_level)
logger = logging.getLogger("bpmn_ai_copilot")

app = FastAPI(
    title=settings.app_name,
    version=settings.app_version,
    description="Microservicio IA para crear y modificar diagramas BPMN (JointJS).",
)

app.add_middleware(
    CORSMiddleware,
    allow_origins=settings.allowed_origins,
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

agent_service = DiagramAgentService(settings=settings)
openai_client = OpenAI(
    api_key=settings.ai_api_key or None,
    timeout=settings.ai_timeout_seconds,
    base_url=settings.ai_base_url or None,
) if settings.ai_api_key else None


class CopilotRequest(BaseModel):
    userMessage: str = Field(..., min_length=1)
    currentDiagram: Dict[str, Any] = Field(default_factory=dict)
    lanes: List[Dict[str, Any]] = Field(default_factory=list)
    context: str | None = None
    models: List[str] = Field(default_factory=list)
    history: List[Dict[str, Any]] = Field(default_factory=list)


class CopilotResponse(BaseModel):
    message: str
    suggested_actions: List[str] = Field(default_factory=list)

class VoiceFillRequest(BaseModel):
    voice_transcript: str = Field(..., min_length=1)
    form_fields: List[str] = Field(default_factory=list)


class PolicyRequirementCandidate(BaseModel):
    id: str = ""
    name: str = ""
    required: bool = False


class PolicyIntentCandidate(BaseModel):
    id: str
    name: str
    description: str = ""
    initial_requirements: List[PolicyRequirementCandidate] = Field(default_factory=list)


class PolicyIntentRequest(BaseModel):
    text: str = Field(..., min_length=1)
    policies: List[PolicyIntentCandidate] = Field(default_factory=list)


class PolicyIntentCandidateResponse(BaseModel):
    policyId: str
    policyName: str
    confidence: float
    missing_requirements: List[str] = Field(default_factory=list)
    reason: str = ""


class PolicyIntentResponse(BaseModel):
    candidates: List[PolicyIntentCandidateResponse] = Field(default_factory=list)


@app.get("/health", response_model=HealthResponse)
def health() -> HealthResponse:
    return HealthResponse(
        status="ok",
        model=settings.ai_model,
        available_models=settings.ai_models,
        ai_provider_configured=agent_service.ai_enabled,
    )


def _resolve_model_candidates(requested_models: List[str]) -> List[str]:
    candidates: List[str] = []
    if requested_models:
        candidates.extend([model.strip() for model in requested_models if model and model.strip()])
    candidates.extend(settings.ai_models)

    deduped: List[str] = []
    seen = set()
    for model in candidates:
        if model in seen:
            continue
        seen.add(model)
        deduped.append(model)
    return deduped or [settings.ai_model]


def _tokenize(value: str) -> set[str]:
    normalized = re.sub(r"[^a-zA-Z0-9]+", " ", value or "").lower()
    return {token for token in normalized.split() if len(token) > 2}


def _lexical_policy_match(text: str, policies: List[PolicyIntentCandidate]) -> PolicyIntentResponse:
    text_tokens = _tokenize(text)
    scored: List[PolicyIntentCandidateResponse] = []

    for policy in policies:
        policy_text = f"{policy.name} {policy.description}"
        policy_tokens = _tokenize(policy_text)
        overlap = len(text_tokens.intersection(policy_tokens)) / max(len(text_tokens.union(policy_tokens)), 1)
        sequence = SequenceMatcher(None, (text or "").lower(), policy_text.lower()).ratio()
        score = max(overlap, sequence * 0.72)
        missing = [
            requirement.name
            for requirement in policy.initial_requirements
            if requirement.required and requirement.name.strip()
        ]
        scored.append(
            PolicyIntentCandidateResponse(
                policyId=policy.id,
                policyName=policy.name,
                confidence=round(min(max(score, 0.0), 0.99), 4),
                missing_requirements=missing,
                reason="Coincidencia calculada con clasificador lexico local.",
            )
        )

    scored.sort(key=lambda item: item.confidence, reverse=True)
    return PolicyIntentResponse(candidates=scored[:3])


@app.post("/api/v1/agent/policy-intent", response_model=PolicyIntentResponse)
def classify_policy_intent(payload: PolicyIntentRequest, http_request: Request) -> PolicyIntentResponse:
    request_id = http_request.headers.get("X-Request-Id", "").strip() or "n/a"
    text = (payload.text or "").strip()
    policies = payload.policies or []

    if not text:
        raise HTTPException(status_code=400, detail="text es obligatorio")
    if not policies:
        raise HTTPException(status_code=400, detail="policies es obligatorio")

    if openai_client is None:
        logger.warning("policy_intent fallback: AI_API_KEY no configurado request_id=%s", request_id)
        return _lexical_policy_match(text, policies)

    system_prompt = (
        "Eres un agente de recepción inteligente para un iBPMS. "
        "Debes mapear la necesidad escrita o hablada del cliente contra el catálogo de trámites disponibles. "
        "Devuelve un JSON con la clave candidates, que debe ser una lista de hasta 3 opciones. "
        "Cada opcion debe tener: policyId, policyName, confidence, missing_requirements, reason. "
        "confidence debe estar entre 0 y 1. "
        "missing_requirements debe contener los nombres de requisitos iniciales obligatorios de cada politica. "
        "No inventes policyId. Si no hay coincidencias razonables, devuelve candidates vacio."
    )
    user_payload = {
        "client_text": text,
        "policies": [policy.model_dump() for policy in policies],
    }

    try:
        completion = None
        selected_model = ""
        last_error: Exception | None = None
        for model in _resolve_model_candidates([]):
            try:
                completion = openai_client.chat.completions.create(
                    model=model,
                    temperature=0,
                    response_format={"type": "json_object"},
                    messages=[
                        {"role": "system", "content": system_prompt},
                        {"role": "user", "content": json.dumps(user_payload, ensure_ascii=True)},
                    ],
                )
                selected_model = model
                break
            except Exception as model_exception:
                last_error = model_exception
                logger.warning(
                    "policy_intent model failed request_id=%s model=%s error=%s",
                    request_id,
                    model,
                    model_exception,
                )
                continue

        if completion is None:
            if last_error is not None:
                raise last_error
            raise RuntimeError("No se obtuvo respuesta de ningun modelo configurado.")

        raw = (completion.choices[0].message.content or "").strip()
        parsed = json.loads(raw) if raw else {}
        policy_by_id = {policy.id: policy for policy in policies}
        raw_candidates = parsed.get("candidates", [])
        if not isinstance(raw_candidates, list):
            raw_candidates = [parsed]

        candidates: List[PolicyIntentCandidateResponse] = []
        for raw_candidate in raw_candidates:
            if not isinstance(raw_candidate, dict):
                continue
            policy_id = str(raw_candidate.get("policyId", "")).strip()
            selected_policy = policy_by_id.get(policy_id)
            if selected_policy is None:
                continue
            missing = [
                requirement.name
                for requirement in selected_policy.initial_requirements
                if requirement.required and requirement.name.strip()
            ]
            confidence = float(raw_candidate.get("confidence", 0) or 0)
            candidates.append(
                PolicyIntentCandidateResponse(
                    policyId=selected_policy.id,
                    policyName=selected_policy.name,
                    confidence=round(min(max(confidence, 0.0), 1.0), 4),
                    missing_requirements=missing,
                    reason=str(raw_candidate.get("reason", "")).strip() or "Clasificacion realizada por IA.",
                )
            )

        if not candidates:
            fallback = _lexical_policy_match(text, policies)
            if fallback.candidates:
                fallback.candidates[0].reason = "La IA no devolvio candidatos validos; se aplico fallback lexico."
            return fallback

        candidates.sort(key=lambda item: item.confidence, reverse=True)
        logger.info(
            "policy_intent success request_id=%s model=%s candidates=%s",
            request_id,
            selected_model,
            len(candidates),
        )
        return PolicyIntentResponse(candidates=candidates[:3])
    except Exception as exception:
        logger.exception("policy_intent error request_id=%s", request_id)
        fallback = _lexical_policy_match(text, policies)
        if fallback.candidates:
            fallback.candidates[0].reason = f"Fallback lexico por error IA: {exception.__class__.__name__}"
        return fallback


@app.post("/api/v1/agent/voice-fill")
def voice_fill(payload: VoiceFillRequest, http_request: Request) -> Dict[str, Any]:
    request_id = http_request.headers.get("X-Request-Id", "").strip() or "n/a"
    transcript = (payload.voice_transcript or "").strip()
    fields = [f.strip() for f in (payload.form_fields or []) if isinstance(f, str) and f.strip()]
    if not transcript:
        raise HTTPException(status_code=400, detail="voice_transcript es obligatorio")
    if not fields:
        raise HTTPException(status_code=400, detail="form_fields es obligatorio")

    if openai_client is None:
        logger.warning("voice_fill fallback: AI_API_KEY no configurado request_id=%s", request_id)
        return {}

    system_prompt = (
        "Eres un extractor estructurado de datos para formularios BPMN. "
        "Debes responder UNICAMENTE con un JSON objeto clave-valor. "
        "Reglas: 1) Usa solo claves incluidas en form_fields. "
        "2) No agregues explicaciones, markdown ni texto extra. "
        "3) Si no puedes inferir un campo con confianza, omítelo. "
        "4) No inventes campos."
    )
    user_payload = {
        "form_fields": fields,
        "voice_transcript": transcript,
    }

    try:
        completion = None
        last_error: Exception | None = None
        selected_model = ""
        for model in _resolve_model_candidates([]):
            try:
                completion = openai_client.chat.completions.create(
                    model=model,
                    temperature=0,
                    response_format={"type": "json_object"},
                    messages=[
                        {"role": "system", "content": system_prompt},
                        {"role": "user", "content": json.dumps(user_payload, ensure_ascii=True)},
                    ],
                )
                selected_model = model
                break
            except Exception as model_exception:
                last_error = model_exception
                continue

        if completion is None:
            if last_error is not None:
                raise last_error
            raise RuntimeError("No se obtuvo respuesta de ningun modelo configurado.")

        raw = (completion.choices[0].message.content or "").strip()
        parsed = json.loads(raw) if raw else {}
        if not isinstance(parsed, dict):
            return {}

        allowed = set(fields)
        filtered = {k: v for k, v in parsed.items() if isinstance(k, str) and k in allowed}
        logger.info("voice_fill success request_id=%s model=%s keys=%s", request_id, selected_model, len(filtered))
        return filtered
    except Exception as exception:
        logger.exception("voice_fill error request_id=%s", request_id)
        raise HTTPException(
            status_code=502,
            detail=f"voice_fill.failed request_id={request_id}: {exception.__class__.__name__}: {exception}",
        ) from exception


@app.post("/api/v1/agent/diagram", response_model=AgentResult)
def process_diagram(request: AgentRequest) -> AgentResult:
    if request.operation == "modify" and not request.current_diagram:
        raise HTTPException(
            status_code=400,
            detail="current_diagram es obligatorio cuando operation=modify",
        )
    logger.info(
        "agent_diagram request received operation=%s instruction_len=%s lanes=%s cells=%s",
        request.operation,
        len(request.instruction or ""),
        len(request.lanes or []),
        len((request.current_diagram or {}).get("cells") or []),
    )
    result = agent_service.process(request)
    logger.info(
        "agent_diagram response operation=%s result_lanes=%s result_cells=%s warnings=%s",
        result.operation,
        len(result.lanes or []),
        len((result.diagram or {}).get("cells") or []),
        len(result.warnings or []),
    )
    return result


@app.post("/api/ai/copilot-chat", response_model=CopilotResponse)
def copilot_chat(payload: CopilotRequest, http_request: Request) -> CopilotResponse:
    request_id = http_request.headers.get("X-Request-Id", "").strip() or "n/a"
    diagram_cells = payload.currentDiagram.get("cells") if isinstance(payload.currentDiagram, dict) else None
    cell_count = len(diagram_cells) if isinstance(diagram_cells, list) else 0
    logger.info(
        "copilot_chat request received: request_id=%s message_len=%s cell_count=%s lanes=%s has_context=%s model=%s",
        request_id,
        len(payload.userMessage or ""),
        cell_count,
        len(payload.lanes or []),
        bool(payload.context and payload.context.strip()),
        settings.ai_models[0] if settings.ai_models else settings.ai_model,
    )

    if openai_client is None:
        logger.warning("copilot_chat fallback: AI_API_KEY no configurado.")
        return CopilotResponse(
            message=(
                "El proveedor IA no esta configurado. Define AI_API_KEY en el .env "
                "para activar respuestas inteligentes."
            ),
            suggested_actions=[
                "Configura AI_API_KEY en el microservicio bpmn-ai-engine.",
                "Vuelve a intentar el analisis del diagrama.",
            ],
        )

    system_prompt = (
        "Eres un mentor experto en BPMN. El usuario está diseñando un proceso. Te adjunto el JSON actual de su lienzo JointJS. \n"
        "   Tus reglas: \n"
        "   1. Responde preguntas usando buenas prácticas BPMN. \n"
        "   2. Si el lienzo está vacío, sugiere cómo empezar. \n"
        "   3. Si ves errores en el JSON (ej. nodos de decisión sin salidas, nodos sin conectar), adviértele amablemente y dile cómo arreglarlo. \n"
        "   Devuelve SIEMPRE un JSON con las claves 'message' (tu respuesta amigable) y 'suggested_actions' (una lista de strings con acciones recomendadas)."
    )

    user_payload = {
        "user_message": payload.userMessage,
        "current_diagram": payload.currentDiagram,
        "lanes": payload.lanes,
        "context": payload.context,
        "conversation_history": payload.history,
    }

    try:
        completion = None
        selected_model = ""
        last_error: Exception | None = None
        for model in _resolve_model_candidates(payload.models):
            try:
                completion = openai_client.chat.completions.create(
                    model=model,
                    temperature=0.2,
                    response_format={"type": "json_object"},
                    messages=[
                        {"role": "system", "content": system_prompt},
                        {"role": "user", "content": json.dumps(user_payload, ensure_ascii=True)},
                    ],
                )
                selected_model = model
                break
            except Exception as model_exception:
                last_error = model_exception
                logger.warning(
                    "copilot_chat model failed request_id=%s model=%s error=%s",
                    request_id,
                    model,
                    model_exception,
                )
                continue

        if completion is None:
            if last_error is not None:
                raise last_error
            raise RuntimeError("No se obtuvo respuesta de ningun modelo configurado.")
    except Exception as exception:
        logger.exception("copilot_chat error in ai_provider_call request_id=%s", request_id)
        raise HTTPException(
            status_code=502,
            detail=f"copilot_chat.ai_provider_call_failed request_id={request_id}: {exception.__class__.__name__}: {exception}",
        ) from exception

    try:
        raw = (completion.choices[0].message.content or "").strip()
    except Exception as exception:
        logger.exception("copilot_chat error reading ai response content request_id=%s", request_id)
        raise HTTPException(
            status_code=500,
            detail=f"copilot_chat.ai_response_read_failed request_id={request_id}: {exception.__class__.__name__}: {exception}",
        ) from exception

    if not raw:
        logger.error("copilot_chat ai returned empty content request_id=%s", request_id)
        return CopilotResponse(
            message="No se recibio contenido desde la IA. Intenta nuevamente.",
            suggested_actions=["Reintentar la consulta del copilot."],
        )

    try:
        parsed = json.loads(raw)
    except Exception as exception:
        logger.exception("copilot_chat error parsing ai json request_id=%s", request_id)
        raise HTTPException(
            status_code=500,
            detail=(
                "copilot_chat.ai_json_parse_failed: "
                f"request_id={request_id} {exception.__class__.__name__}: {exception}. raw={raw[:500]}"
            ),
        ) from exception

    message = str(parsed.get("message", "")).strip() or "Analisis completado."
    actions_raw = parsed.get("suggested_actions", [])
    suggested_actions = [str(item).strip() for item in actions_raw if str(item).strip()] if isinstance(actions_raw, list) else []
    logger.info(
        "copilot_chat success: request_id=%s model=%s suggested_actions=%s",
        request_id,
        selected_model,
        len(suggested_actions),
    )

    return CopilotResponse(message=message, suggested_actions=suggested_actions)
