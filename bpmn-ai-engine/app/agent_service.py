from __future__ import annotations

import json
import logging
from copy import deepcopy
from typing import Any, Dict, List, Optional

from openai import OpenAI

from .config import Settings
from .diagram_tools import create_default_diagram, sanitize_diagram
from .models import AgentRequest, AgentResult

logger = logging.getLogger("bpmn_ai_agent")


class DiagramAgentService:
    def __init__(self, settings: Settings) -> None:
        self.settings = settings
        self.client: Optional[OpenAI] = None
        if settings.ai_api_key:
            client_kwargs: Dict[str, Any] = {
                "api_key": settings.ai_api_key,
                "timeout": settings.ai_timeout_seconds,
            }
            if settings.ai_base_url:
                client_kwargs["base_url"] = settings.ai_base_url
            self.client = OpenAI(**client_kwargs)

    @property
    def ai_enabled(self) -> bool:
        return self.client is not None

    def _resolve_model_candidates(self, requested_models: Optional[List[str]] = None) -> List[str]:
        candidates: List[str] = []
        if requested_models:
            candidates.extend([model.strip() for model in requested_models if model and model.strip()])
        candidates.extend(self.settings.ai_models)

        deduped: List[str] = []
        seen = set()
        for model in candidates:
            if model in seen:
                continue
            seen.add(model)
            deduped.append(model)
        return deduped or [self.settings.ai_model]

    def process(self, request: AgentRequest) -> AgentResult:
        request_lanes = [lane.model_dump() for lane in request.lanes]
        if request.operation == "create":
            base_diagram = create_default_diagram()
        else:
            base_diagram = deepcopy(request.current_diagram or create_default_diagram())

        logger.info(
            "agent.process.start operation=%s instruction_len=%s request_lanes=%s base_cells=%s ai_enabled=%s",
            request.operation,
            len(request.instruction or ""),
            len(request_lanes),
            len((base_diagram or {}).get("cells") or []),
            self.ai_enabled,
        )
        logger.debug("agent.process.request_lanes=%s", json.dumps(request_lanes, ensure_ascii=True))

        if self.client is None:
            logger.warning("agent.process.fallback ai_client_not_configured")
            return self._fallback_result(request, base_diagram)

        try:
            ai_result = self._run_llm(request, base_diagram)
            ai_lanes = self._normalize_lanes(ai_result.get("lanes"), request_lanes)

            ai_diagram = ai_result.get("diagram") or {}
            merged_diagram = ai_diagram if self._is_destructive_instruction(request.instruction) else self._merge_diagrams(base_diagram, ai_diagram)
            if not ai_lanes:
                ai_lanes = self._infer_lanes_from_diagram(merged_diagram, request_lanes)
            normalized_diagram, sanitize_warnings = sanitize_diagram(
                merged_diagram or base_diagram,
                ai_lanes,
            )

            warnings = list(ai_result.get("warnings") or [])
            warnings.extend(sanitize_warnings)

            logger.info(
                "agent.process.success operation=%s result_cells=%s result_lanes=%s warnings=%s",
                request.operation,
                len((normalized_diagram or {}).get("cells") or []),
                len(ai_lanes),
                len(warnings),
            )
            logger.debug("agent.process.result_lanes=%s", json.dumps(ai_lanes, ensure_ascii=True))

            return AgentResult(
                operation=request.operation,
                summary=(ai_result.get("summary") or "Diagrama procesado correctamente.").strip(),
                changes=list(ai_result.get("changes") or []),
                warnings=warnings,
                diagram=normalized_diagram,
                lanes=ai_lanes,
            )
        except Exception as exc:
            logger.exception("agent.process.error operation=%s", request.operation)
            fallback = self._fallback_result(request, base_diagram)
            fallback.warnings.append(f"Error en IA: {str(exc)}")
            return fallback

    def _is_destructive_instruction(self, instruction: str) -> bool:
        normalized = (instruction or "").lower()
        return any(term in normalized for term in ["elimina", "borra", "quita", "remueve", "limpia", "desconecta"])

    def _merge_diagrams(self, base_diagram: Dict[str, Any], ai_diagram: Dict[str, Any]) -> Dict[str, Any]:
        base_cells = list((base_diagram or {}).get("cells") or [])
        ai_cells = list((ai_diagram or {}).get("cells") or [])

        merged_by_id: Dict[str, Dict[str, Any]] = {}
        order: List[str] = []

        for cell in base_cells:
            if not isinstance(cell, dict):
                continue
            cell_id = str(cell.get("id") or "").strip()
            if not cell_id:
                continue
            merged_by_id[cell_id] = cell
            order.append(cell_id)

        for cell in ai_cells:
            if not isinstance(cell, dict):
                continue
            cell_id = str(cell.get("id") or "").strip()
            if not cell_id:
                continue
            merged_by_id[cell_id] = cell
            if cell_id not in order:
                order.append(cell_id)

        merged_cells = [merged_by_id[cell_id] for cell_id in order if cell_id in merged_by_id]
        return {"cells": merged_cells}

    def _normalize_lanes(
        self,
        raw_lanes: Any,
        fallback_lanes: List[Dict[str, Any]],
    ) -> List[Dict[str, Any]]:
        source = raw_lanes if isinstance(raw_lanes, list) and raw_lanes else fallback_lanes
        normalized: List[Dict[str, Any]] = []
        seen_ids: set[str] = set()
        default_width = 600.0
        default_height = 800.0

        for index, lane in enumerate(source or []):
            if not isinstance(lane, dict):
                continue

            lane_id = str(lane.get("id") or lane.get("_id") or "").strip()
            lane_name = str(lane.get("name") or lane_id).strip()
            if not lane_id or lane_id in seen_ids:
                continue

            x_raw = lane.get("x")
            width_raw = lane.get("width")
            height_raw = lane.get("height")

            x = float(x_raw) if isinstance(x_raw, (int, float)) else index * default_width
            width = float(width_raw) if isinstance(width_raw, (int, float)) and float(width_raw) > 0 else default_width
            height = float(height_raw) if isinstance(height_raw, (int, float)) and float(height_raw) > 0 else default_height

            normalized.append(
                {
                    "id": lane_id,
                    "name": lane_name or lane_id,
                    "color": str(lane.get("color") or "#dbeafe"),
                    "x": round(x, 2),
                    "width": round(width, 2),
                    "height": round(height, 2),
                }
            )
            seen_ids.add(lane_id)

        return normalized

    def _infer_lanes_from_diagram(
        self,
        diagram: Dict[str, Any],
        fallback_lanes: List[Dict[str, Any]],
    ) -> List[Dict[str, Any]]:
        lane_ids_in_nodes: List[str] = []
        for cell in (diagram or {}).get("cells") or []:
            if not isinstance(cell, dict):
                continue
            if cell.get("type") == "standard.Link":
                continue
            lane_id = str(cell.get("laneId") or "").strip()
            if lane_id and lane_id not in lane_ids_in_nodes:
                lane_ids_in_nodes.append(lane_id)

        if not lane_ids_in_nodes:
            return self._normalize_lanes(fallback_lanes, [])

        normalized_fallback = self._normalize_lanes(fallback_lanes, [])
        fallback_by_id = {lane.get("id"): lane for lane in normalized_fallback if lane.get("id")}

        inferred: List[Dict[str, Any]] = []
        default_width = 600.0
        default_height = 800.0
        for index, lane_id in enumerate(lane_ids_in_nodes):
            matched = fallback_by_id.get(lane_id)
            if matched:
                inferred.append(matched)
                continue
            inferred.append(
                {
                    "id": lane_id,
                    "name": lane_id,
                    "color": "#dbeafe",
                    "x": round(index * default_width, 2),
                    "width": default_width,
                    "height": default_height,
                }
            )
        return inferred

    def _run_llm(self, request: AgentRequest, base_diagram: Dict[str, Any]) -> Dict[str, Any]:
        system_prompt = """Eres el motor de IA de un modelador BPMN (JointJS + Spring Boot).
Debes responder SIEMPRE con JSON VALIDO y compatible con este backend.

FORMATO DE RESPUESTA OBLIGATORIO (SIN TEXTO ADICIONAL):
{
  "summary": "texto",
  "changes": ["..."],
  "warnings": [],
  "lanes": [
    {"id":"rrhh","name":"rrhh","color":"#E8F4FD","x":300,"width":600,"height":800}
  ],
  "diagram": {"cells": [ ... ]}
}

REGLAS ESTRICTAS:
1) NO serialices diagram como string. Debe ser objeto JSON con clave "cells".
2) NO crees celdas visuales de carriles en diagram.cells.
3) Tipos de nodo permitidos: START, TASK, DECISION, FORK, JOIN, SYNCHRONIZATION, END.
4) Cada nodo DEBE tener:
   - id, type, nodeType, laneId, position{x,y}, size{width,height}, attrs{body,label}
5) TASK debe incluir nodeMeta.taskForm con:
   - title, description, fields (array), attachments (array)
6) DECISION debe incluir nodeMeta.decisionExpression (string, puede ser vacio)
7) Cada link debe tener:
   - id, type=standard.Link, source{id}, target{id}, z=0
   - attrs.line.stroke, strokeWidth, targetMarker
   - router {name:"orthogonal", args:{padding:30}}
8) Si link sale desde DECISION:
   - conditionLabel obligatorio
   - condition {"type":"expression","script":"#_decisionTomada == '<label>'"}
   - labels[0].attrs.text.text con el mismo valor de conditionLabel
9) lanes debe ser coherente con laneId usado en nodos. Si agregas nodos en nuevos carriles, crea esos carriles en "lanes".
10) Los lanes son columnas verticales:
   - x = centro horizontal del carril
   - width > 0, height > 0
   - nodos deben quedar dentro de su carril.

CRITERIOS ANTI-ERROR:
- No dejes source/target huerfanos.
- No repitas ids.
- Incluye al menos un START y un END si la instruccion describe flujo completo.
- Conserva nodos existentes si la instruccion es aditiva.
"""

        user_payload = {
            "operation": request.operation,
            "instruction": request.instruction,
            "lanes": [lane.model_dump() for lane in request.lanes],
            "current_diagram": base_diagram,
            "context": request.context,
        }

        last_error: Optional[Exception] = None
        raw_text = ""
        for model in self._resolve_model_candidates(request.models):
            try:
                logger.info("agent.run_llm.model_try model=%s", model)
                response = self.client.chat.completions.create(
                    model=model,
                    temperature=0.1,
                    response_format={"type": "json_object"},
                    messages=[
                        {"role": "system", "content": system_prompt},
                        {"role": "user", "content": json.dumps(user_payload, ensure_ascii=True)},
                    ],
                )
                raw_text = (response.choices[0].message.content or "").strip()
                if raw_text:
                    logger.info("agent.run_llm.model_success model=%s raw_len=%s", model, len(raw_text))
                    break
            except Exception as exc:
                last_error = exc
                logger.warning("agent.run_llm.model_fail model=%s error=%s", model, exc)
                continue

        if not raw_text:
            if last_error:
                raise last_error
            raise RuntimeError("La IA no devolvio contenido.")

        start_idx = raw_text.find("{")
        end_idx = raw_text.rfind("}")
        clean_json_str = raw_text[start_idx : end_idx + 1] if start_idx != -1 and end_idx != -1 and end_idx > start_idx else raw_text

        try:
            parsed = json.loads(clean_json_str)
        except Exception as exc:
            raise RuntimeError(f"Error parseando JSON: {exc}. Texto recibido: {clean_json_str[:2000]}")

        if "diagram" not in parsed:
            raise RuntimeError("La IA no retorno la clave 'diagram'.")

        logger.info(
            "agent.run_llm.parsed cells=%s lanes=%s changes=%s warnings=%s",
            len((parsed.get("diagram") or {}).get("cells") or []),
            len(parsed.get("lanes") or []),
            len(parsed.get("changes") or []),
            len(parsed.get("warnings") or []),
        )
        logger.debug("agent.run_llm.raw_response=%s", raw_text[:4000])
        logger.debug("agent.run_llm.parsed_lanes=%s", json.dumps(parsed.get("lanes") or [], ensure_ascii=True))

        return parsed

    def _fallback_result(self, request: AgentRequest, base_diagram: Dict[str, Any]) -> AgentResult:
        original_lanes = self._normalize_lanes([lane.model_dump() for lane in request.lanes], [])
        diagram, warnings = sanitize_diagram(base_diagram, original_lanes)
        summary = "Modo fallback: Se mantuvo el diagrama original ante un error."

        return AgentResult(
            operation=request.operation,
            summary=summary,
            changes=["Saneamiento de seguridad aplicado."],
            warnings=warnings,
            diagram=diagram,
            lanes=original_lanes,
        )
