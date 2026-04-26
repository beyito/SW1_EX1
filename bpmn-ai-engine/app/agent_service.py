from __future__ import annotations

import json
from copy import deepcopy
from typing import Any, Dict, List, Optional

from openai import OpenAI
from pydantic import ValidationError

from .config import Settings
from .diagram_tools import ALLOWED_NODE_TYPES, create_default_diagram, sanitize_diagram
from .models import AgentRequest, AgentResult


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
        if request.operation == "create":
            base_diagram = create_default_diagram()
        else:
            base_diagram = deepcopy(request.current_diagram or create_default_diagram())

        if self.client is None:
            return self._fallback_result(request, base_diagram)

        try:
            ai_result = self._run_llm(request, base_diagram)

            ai_diagram = ai_result.get("diagram") or {}
            merged_diagram = ai_diagram if self._is_destructive_instruction(request.instruction) else self._merge_diagrams(base_diagram, ai_diagram)
            normalized_diagram, sanitize_warnings = sanitize_diagram(
                merged_diagram or base_diagram,
                [lane.model_dump() for lane in request.lanes],
            )
            
            warnings = list(ai_result.get("warnings") or [])
            warnings.extend(sanitize_warnings)

            return AgentResult(
                operation=request.operation,
                summary=(ai_result.get("summary") or "Diagrama procesado correctamente.").strip(),
                changes=list(ai_result.get("changes") or []),
                warnings=warnings,
                diagram=normalized_diagram,
            )
        except Exception as exc:
            print(f"Error en procesamiento IA: {exc}")
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

    def _run_llm(self, request: AgentRequest, base_diagram: Dict[str, Any]) -> Dict[str, Any]:
        system_prompt = f"""Eres el motor de Inteligencia Artificial de un modelador corporativo BPMN en Angular usando JointJS.
Tu objetivo es traducir las instrucciones del usuario en un diagrama JSON ESTRICTAMENTE VÁLIDO.

REGLA 1: ESTRUCTURA EXACTA DEL JSON DE JOINTJS
Si te equivocas en el formato, el lienzo se rompe o los nodos se ven minúsculos.
- 'size' SIEMPRE debe ser un objeto: "size": {{"width": 140, "height": 70}}
- 'position' SIEMPRE debe ser un objeto: "position": {{"x": 100, "y": 200}}
- 'source' y 'target' en los enlaces SIEMPRE deben ser objetos: "source": {{"id": "nodo_1"}}

REGLA 2: EJEMPLOS ESTRICTOS DE CELDAS (NODOS)
CADA nodo que crees DEBE ser un objeto dentro de la lista "cells" y seguir este formato estricto:

Ejemplo de Tarea (TASK):
{{
  "type": "standard.Rectangle",
  "id": "task_1",
  "position": {{"x": 300, "y": 150}},
  "size": {{"width": 140, "height": 70}},
  "z": 10,
  "laneId": "id_del_carril_real",
  "attrs": {{
    "body": {{"fill": "#eff6ff", "stroke": "#3b82f6", "rx": 8, "ry": 8}},
    "label": {{"text": "Nombre Tarea", "fill": "#1e3a8a", "textWrap": {{"width": 120}}}}
  }},
  "nodeMeta": {{"taskForm": {{"title": "", "description": "", "fields": [], "attachments": []}}}}
}}

Otros nodos (solo cambia 'type', 'size' y 'attrs'):
- Inicio (START): type "standard.Circle", size {{"width": 80, "height": 80}}, attrs.body {{"fill": "#d1fae5", "stroke": "#10b981"}}
- Fin (END): type "standard.Circle", size {{"width": 90, "height": 90}}, attrs.body {{"fill": "#fee2e2", "stroke": "#ef4444", "strokeWidth": 4}}
- Decisión (DECISION): type "standard.Polygon", size {{"width": 120, "height": 120}}, attrs.body {{"refPoints": "0,60 60,0 120,60 60,120", "fill": "#fbbf24"}}
- Bifurcación (FORK): type "standard.Rectangle", size {{"width": 20, "height": 160}}, attrs.body {{"fill": "#1f2937", "stroke": "#1f2937", "rx": 0, "ry": 0}}
- Sincronización (JOIN/SYNC): type "standard.Rectangle", size {{"width": 20, "height": 160}}, attrs.body {{"fill": "#1f2937", "stroke": "#1f2937", "rx": 0, "ry": 0}}

REGLA 3: ENLACES (LINKS) OBLIGATORIOS
Para conectar los pasos, DEBES crear celdas de enlace al final de la lista "cells":
{{
  "type": "standard.Link",
  "id": "link_1",
  "source": {{"id": "origen_id"}},
  "target": {{"id": "destino_id"}},
  "z": 0,
  "router": {{"name": "orthogonal", "args": {{"padding": 30}}}},
  "connector": {{"name": "straight"}},
  "attrs": {{
    "line": {{"stroke": "#0f172a", "strokeWidth": 2, "targetMarker": {{"type": "path", "d": "M 10 -5 0 0 10 5 z"}}}}
  }}
}}
IMPORTANTE: Si la flecha sale de un nodo DECISION, agrégale esta propiedad al enlace:
"labels": [ {{"position": 0.5, "attrs": {{"text": {{"text": "Sí/No"}}}}}} ]

REGLA 4: COORDENADAS DINÁMICAS Y CARRILES (SWIMLANES)
¡Los carriles varían por empresa! Lee el array 'lanes' que viene en el payload del usuario.
- Lógica Vertical (Y): Determina a qué carril pertenece la tarea. Ubica la posición de ese carril en el array (índice 0, 1, 2, etc.). Usa esta fórmula mental: (índice_del_carril * 250) + 100.
  Ejemplo: El carril en el índice 0 del array va en "y": 100. El carril en el índice 1 va en "y": 350. El carril en el índice 2 va en "y": 600. 
  Asigna SIEMPRE el "laneId" exacto con el ID del objeto lane correspondiente.
- Lógica Horizontal (X): Incrementa la 'x' en 200 píxeles por cada nuevo paso en el flujo (ej: Paso 1 en x:100, Paso 2 en x:300, Paso 3 en x:500) para que el diagrama avance hacia la derecha de forma limpia.

Devuelve SOLO un JSON con esta estructura exacta:
{{
  "summary": "Resumen en texto",
  "changes": ["Cambio 1", "Cambio 2"],
  "warnings": [],
  "diagram": {{ "cells": [ /* Aquí van TODOS los nodos y enlaces */ ] }}
}}
"""

        user_payload = {
            "operation": request.operation,
            "instruction": request.instruction,
            "lanes": [lane.model_dump() for lane in request.lanes],
            "current_diagram": base_diagram,
        }

        last_error: Optional[Exception] = None
        raw_text = ""
        for model in self._resolve_model_candidates(request.models):
            try:
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
                    break
            except Exception as exc:
                last_error = exc
                continue

        if not raw_text:
            if last_error:
                raise last_error
            raise RuntimeError("La IA no devolvio contenido.")
        
        # Limpieza de markdown usando replace para evitar cortes
        if "```json" in raw_text:
            raw_text = raw_text.replace("```json", "", 1)
        if "```" in raw_text:
            raw_text = raw_text.replace("```", "")
        raw_text = raw_text.strip()

        parsed = json.loads(raw_text)
        if "diagram" not in parsed:
            raise RuntimeError("La IA no retornó la clave 'diagram'.")
        return parsed

    def _fallback_result(self, request: AgentRequest, base_diagram: Dict[str, Any]) -> AgentResult:
        diagram, warnings = sanitize_diagram(base_diagram, [lane.model_dump() for lane in request.lanes])
        summary = "Modo fallback: Se mantuvo el diagrama original ante un error."
        return AgentResult(
            operation=request.operation,
            summary=summary,
            changes=["Saneamiento de seguridad aplicado."],
            warnings=warnings,
            diagram=diagram,
        )
