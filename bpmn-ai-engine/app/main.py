from __future__ import annotations

import json
import logging
import re
from difflib import SequenceMatcher
from datetime import datetime
from typing import Any, Dict, List

from fastapi import FastAPI, HTTPException, Request
from fastapi.middleware.cors import CORSMiddleware
from openai import OpenAI
from pydantic import BaseModel, Field

from .agent_service import DiagramAgentService
from .config import get_settings
from .models import AgentRequest, AgentResult, HealthResponse

try:
    import numpy as np
    import tensorflow as tf
    from tensorflow import keras

    TENSORFLOW_AVAILABLE = True
except Exception:
    np = None
    tf = None
    keras = None
    TENSORFLOW_AVAILABLE = False

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


class PredictiveProcessEvent(BaseModel):
    processInstanceId: str
    policyId: str = ""
    title: str = ""
    description: str = ""
    status: str = ""
    startedBy: str = ""
    startedAt: str | None = None
    completedAt: str | None = None


class PredictiveTaskEvent(BaseModel):
    processInstanceId: str
    policyId: str = ""
    taskId: str = ""
    laneId: str = ""
    status: str = ""
    createdAt: str | None = None
    startedAt: str | None = None
    completedAt: str | None = None


class PredictiveDocumentEvent(BaseModel):
    processInstanceId: str
    policyId: str = ""
    documentId: str = ""
    createdBy: str = ""
    createdAt: str | None = None
    size: float = 0


class PredictiveAnalysisRequest(BaseModel):
    processes: List[PredictiveProcessEvent] = Field(default_factory=list)
    tasks: List[PredictiveTaskEvent] = Field(default_factory=list)
    documents: List[PredictiveDocumentEvent] = Field(default_factory=list)


class PredictiveAnalysisResponse(BaseModel):
    model_strategy: str
    anomalies: List[Dict[str, Any]] = Field(default_factory=list)
    priorities: List[Dict[str, Any]] = Field(default_factory=list)
    route_predictions: List[Dict[str, Any]] = Field(default_factory=list)
    bottlenecks: List[Dict[str, Any]] = Field(default_factory=list)
    warnings: List[str] = Field(default_factory=list)


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


def _parse_datetime(value: str | None) -> datetime | None:
    if not value:
        return None
    try:
        return datetime.fromisoformat(value.replace("Z", "+00:00").replace("+00:00", ""))
    except Exception:
        return None


def _minutes_between(start: str | None, end: str | None) -> float:
    start_dt = _parse_datetime(start)
    end_dt = _parse_datetime(end)
    if not start_dt or not end_dt or end_dt < start_dt:
        return 0.0
    return max((end_dt - start_dt).total_seconds() / 60.0, 0.0)


def _status_score(status: str) -> float:
    return {
        "PENDING": 1.0,
        "IN_PROGRESS": 2.0,
        "COMPLETED": 3.0,
        "REJECTED": 4.0,
        "ACTIVE": 2.0,
        "CANCELLED": 4.0,
    }.get((status or "").upper(), 0.0)


def _priority_label(score: float) -> str:
    if score >= 0.82:
        return "CRITICA"
    if score >= 0.62:
        return "ALTA"
    if score >= 0.35:
        return "MEDIA"
    return "BAJA"


def _build_task_feature(task: PredictiveTaskEvent) -> List[float]:
    wait_minutes = _minutes_between(task.createdAt, task.startedAt)
    execution_minutes = _minutes_between(task.startedAt, task.completedAt)
    age_minutes = _minutes_between(task.createdAt, datetime.utcnow().isoformat()) if task.completedAt is None else 0.0
    return [
        wait_minutes,
        execution_minutes,
        age_minutes,
        _status_score(task.status),
        float(len(task.laneId or "")),
        float(len(task.taskId or "")),
    ]


def _autoencoder_anomalies(tasks: List[PredictiveTaskEvent]) -> tuple[List[Dict[str, Any]], str]:
    features = [_build_task_feature(task) for task in tasks]
    if len(features) < 8 or not TENSORFLOW_AVAILABLE or np is None or keras is None:
        return _heuristic_task_anomalies(tasks), "heuristic"

    try:
        values = np.array(features, dtype="float32")
        mean = values.mean(axis=0)
        std = values.std(axis=0) + 1e-6
        normalized = (values - mean) / std

        model = keras.Sequential(
            [
                keras.layers.Input(shape=(normalized.shape[1],)),
                keras.layers.Dense(4, activation="relu"),
                keras.layers.Dense(2, activation="relu"),
                keras.layers.Dense(4, activation="relu"),
                keras.layers.Dense(normalized.shape[1], activation="linear"),
            ]
        )
        model.compile(optimizer="adam", loss="mse")
        model.fit(normalized, normalized, epochs=12, batch_size=min(8, len(features)), verbose=0)
        reconstructed = model.predict(normalized, verbose=0)
        errors = np.mean(np.square(normalized - reconstructed), axis=1)
        threshold = float(errors.mean() + (2.0 * errors.std()))

        anomalies: List[Dict[str, Any]] = []
        for task, error in zip(tasks, errors):
            if float(error) > threshold:
                anomalies.append(
                    {
                        "type": "TASK_DURATION_ANOMALY",
                        "processInstanceId": task.processInstanceId,
                        "taskId": task.taskId,
                        "laneId": task.laneId,
                        "score": round(float(error), 4),
                        "reason": "El tiempo de espera, duración o ruta de esta tarea se aleja mucho del comportamiento normal registrado.",
                    }
                )
        return anomalies, "tensorflow_autoencoder"
    except Exception as exception:
        logger.warning("predictive autoencoder failed, using heuristic fallback: %s", exception)
        return _heuristic_task_anomalies(tasks), "heuristic"


def _heuristic_task_anomalies(tasks: List[PredictiveTaskEvent]) -> List[Dict[str, Any]]:
    durations = [
        _minutes_between(task.startedAt, task.completedAt)
        for task in tasks
        if _minutes_between(task.startedAt, task.completedAt) > 0
    ]
    if not durations:
        return []
    sorted_durations = sorted(durations)
    median = sorted_durations[len(sorted_durations) // 2]
    threshold = max(median * 3.0, 120.0)
    anomalies: List[Dict[str, Any]] = []
    for task in tasks:
        duration = _minutes_between(task.startedAt, task.completedAt)
        age = _minutes_between(task.createdAt, datetime.utcnow().isoformat()) if (task.status or "").upper() != "COMPLETED" else 0
        if duration > threshold or age > threshold:
            anomalies.append(
                {
                    "type": "TASK_DURATION_ANOMALY",
                    "processInstanceId": task.processInstanceId,
                    "taskId": task.taskId,
                    "laneId": task.laneId,
                    "score": round(max(duration, age) / threshold, 4),
                    "reason": "La tarea tardó o esperó mucho más que casos similares del histórico.",
                }
            )
    return anomalies[:20]


def _document_burst_anomalies(documents: List[PredictiveDocumentEvent]) -> List[Dict[str, Any]]:
    by_process_minute: Dict[str, Dict[str, int]] = {}
    for document in documents:
        created = _parse_datetime(document.createdAt)
        if not created:
            continue
        minute_key = created.strftime("%Y-%m-%dT%H:%M")
        process_bucket = by_process_minute.setdefault(document.processInstanceId, {})
        process_bucket[minute_key] = process_bucket.get(minute_key, 0) + 1

    anomalies: List[Dict[str, Any]] = []
    for process_id, minute_counts in by_process_minute.items():
        for minute_key, count in minute_counts.items():
            if count >= 20:
                anomalies.append(
                    {
                        "type": "DOCUMENT_BURST",
                        "processInstanceId": process_id,
                        "score": count,
                        "reason": f"Se detectaron {count} documentos subidos en el minuto {minute_key}.",
                    }
                )
    return anomalies


def _predict_priorities(processes: List[PredictiveProcessEvent], tasks: List[PredictiveTaskEvent]) -> List[Dict[str, Any]]:
    tasks_by_process: Dict[str, List[PredictiveTaskEvent]] = {}
    for task in tasks:
        tasks_by_process.setdefault(task.processInstanceId, []).append(task)

    priorities: List[Dict[str, Any]] = []
    urgency_terms = {"urgente", "emergencia", "critico", "critica", "hoy", "inmediato", "reclamo", "perdi", "perdida"}
    for process in processes:
        process_tasks = tasks_by_process.get(process.processInstanceId, [])
        pending = sum(1 for task in process_tasks if (task.status or "").upper() in {"PENDING", "IN_PROGRESS"})
        rejected = sum(1 for task in process_tasks if (task.status or "").upper() == "REJECTED")
        text_tokens = _tokenize(f"{process.title} {process.description}")
        urgency = len(text_tokens.intersection(urgency_terms)) / max(len(urgency_terms), 1)
        age_hours = _minutes_between(process.startedAt, datetime.utcnow().isoformat()) / 60.0 if process.completedAt is None else 0.0
        score = min(1.0, (pending * 0.16) + (rejected * 0.22) + min(age_hours / 168.0, 0.35) + (urgency * 0.45))
        priorities.append(
            {
                "processInstanceId": process.processInstanceId,
                "policyId": process.policyId,
                "priority": _priority_label(score),
                "score": round(score, 4),
                "reason": "Prioridad estimada por carga pendiente, antigüedad del trámite y señales de urgencia.",
            }
        )
    priorities.sort(key=lambda item: item["score"], reverse=True)
    return priorities


def _predict_routes(tasks: List[PredictiveTaskEvent]) -> List[Dict[str, Any]]:
    by_process: Dict[str, List[PredictiveTaskEvent]] = {}
    for task in tasks:
        by_process.setdefault(task.processInstanceId, []).append(task)

    transitions: Dict[str, Dict[str, int]] = {}
    for process_tasks in by_process.values():
        ordered = sorted(process_tasks, key=lambda task: task.createdAt or "")
        for left, right in zip(ordered, ordered[1:]):
            bucket = transitions.setdefault(left.taskId, {})
            bucket[right.taskId] = bucket.get(right.taskId, 0) + 1

    predictions: List[Dict[str, Any]] = []
    for current_task, next_counts in transitions.items():
        total = sum(next_counts.values())
        if total <= 0:
            continue
        next_task, count = max(next_counts.items(), key=lambda item: item[1])
        predictions.append(
            {
                "currentTaskId": current_task,
                "predictedNextTaskId": next_task,
                "probability": round(count / total, 4),
                "reason": "Predicción basada en las rutas que más se repiten en el histórico.",
            }
        )
    predictions.sort(key=lambda item: item["probability"], reverse=True)
    return predictions[:20]


def _predict_bottlenecks(tasks: List[PredictiveTaskEvent]) -> List[Dict[str, Any]]:
    by_lane: Dict[str, Dict[str, float]] = {}
    for task in tasks:
        lane = task.laneId or "sin-carril"
        stats = by_lane.setdefault(lane, {"pending": 0, "completed": 0, "wait_total": 0.0, "wait_samples": 0})
        status = (task.status or "").upper()
        if status in {"PENDING", "IN_PROGRESS"}:
            stats["pending"] += 1
        if status == "COMPLETED":
            stats["completed"] += 1
        wait = _minutes_between(task.createdAt, task.startedAt)
        if wait > 0:
            stats["wait_total"] += wait
            stats["wait_samples"] += 1

    bottlenecks: List[Dict[str, Any]] = []
    for lane, stats in by_lane.items():
        avg_wait = stats["wait_total"] / stats["wait_samples"] if stats["wait_samples"] else 0.0
        pressure = stats["pending"] / max(stats["completed"], 1)
        risk = min(1.0, (pressure * 0.35) + min(avg_wait / 480.0, 0.65))
        if risk >= 0.25:
            bottlenecks.append(
                {
                    "laneId": lane,
                    "risk": round(risk, 4),
                    "pendingTasks": int(stats["pending"]),
                    "avgWaitMinutes": round(avg_wait, 2),
                    "reason": "Riesgo calculado por tareas pendientes y espera promedio del carril.",
                }
            )
    bottlenecks.sort(key=lambda item: item["risk"], reverse=True)
    return bottlenecks


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


@app.post("/api/v1/prediction/analyze", response_model=PredictiveAnalysisResponse)
def predictive_analysis(payload: PredictiveAnalysisRequest, http_request: Request) -> PredictiveAnalysisResponse:
    request_id = http_request.headers.get("X-Request-Id", "").strip() or "n/a"
    logger.info(
        "predictive_analysis request_id=%s processes=%s tasks=%s documents=%s tensorflow=%s",
        request_id,
        len(payload.processes),
        len(payload.tasks),
        len(payload.documents),
        TENSORFLOW_AVAILABLE,
    )

    task_anomalies, strategy = _autoencoder_anomalies(payload.tasks)
    document_anomalies = _document_burst_anomalies(payload.documents)
    priorities = _predict_priorities(payload.processes, payload.tasks)
    route_predictions = _predict_routes(payload.tasks)
    bottlenecks = _predict_bottlenecks(payload.tasks)

    warnings: List[str] = []
    if not TENSORFLOW_AVAILABLE:
        warnings.append("TensorFlow no está instalado en el motor IA; se usó cálculo heurístico.")
    if len(payload.tasks) < 8:
        warnings.append("Histórico insuficiente para entrenar autoencoder con buena confianza.")

    return PredictiveAnalysisResponse(
        model_strategy=strategy,
        anomalies=[*task_anomalies, *document_anomalies],
        priorities=priorities,
        route_predictions=route_predictions,
        bottlenecks=bottlenecks,
        warnings=warnings,
    )


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
