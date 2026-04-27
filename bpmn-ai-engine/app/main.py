from __future__ import annotations

import json
import logging
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
