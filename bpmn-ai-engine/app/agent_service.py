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
        system_prompt = f"""Eres el motor de Inteligencia Artificial de un modelador corporativo BPMN en Angular.
Tu objetivo es traducir las instrucciones del usuario en un JSON ESTRICTAMENTE VÁLIDO. 
El diagrama NO es solo visual, es procesado por un WorkflowEngine en Spring Boot, por lo que los atributos personalizados son de vida o muerte.

REGLA 1: ATRIBUTOS RAÍZ OBLIGATORIOS
CADA nodo que crees DEBE tener en su nivel raíz los siguientes atributos: 'id', 'type', 'nodeType' (ESTRICTO), 'laneId' y 'position'.

REGLA 2: ESTRUCTURA EXACTA DE NODOS (CELDAS)
Copia esta estructura para cada tipo. NUNCA omitas el 'nodeType' ni el 'nodeMeta'. ¡NO crees celdas para los carriles (lanes), esos van en un objeto separado!

- TAREA (TASK):
{{
  "type": "standard.Rectangle", "id": "task_1", "nodeType": "TASK", "laneId": "id_del_carril",
  "position": {{"x": 100, "y": 100}}, "size": {{"width": 140, "height": 70}}, "z": 10,
  "attrs": {{ "body": {{"fill": "#eff6ff", "stroke": "#3b82f6", "rx": 8, "ry": 8}}, "label": {{"text": "Nombre Tarea", "fill": "#1f2937", "textWrap": {{"width": 120}}}} }},
  "nodeMeta": {{ "taskForm": {{"title": "","description": "","fields": [],"attachments": []}} }}
}}

- DECISIÓN (DECISION):
{{
  "type": "standard.Polygon", "id": "dec_1", "nodeType": "DECISION", "laneId": "id_del_carril",
  "position": {{"x": 300, "y": 100}}, "size": {{"width": 120, "height": 120}}, "z": 10,
  "attrs": {{ "body": {{"fill": "#fffbeb", "stroke": "#d97706", "refPoints": "0,60 60,0 120,60 60,120"}}, "label": {{"text": "¿Pregunta?"}} }},
  "nodeMeta": {{ "decisionExpression": "" }}
}}

- BIFURCACIÓN (FORK):
{{
  "type": "standard.Rectangle", "id": "fork_1", "nodeType": "FORK", "laneId": "id_del_carril",
  "position": {{"x": 500, "y": 100}}, "size": {{"width": 20, "height": 160}}, "z": 10,
  "attrs": {{ "body": {{"fill": "#000000", "rx": 0, "ry": 0}}, "label": {{"text": "Bifurcación", "fill": "#ffffff"}} }}
}}

- SINCRONIZACIÓN (JOIN):
{{
  "type": "standard.Rectangle", "id": "join_1", "nodeType": "JOIN", "laneId": "id_del_carril",
  "position": {{"x": 700, "y": 100}}, "size": {{"width": 160, "height": 20}}, "z": 10,
  "attrs": {{ "body": {{"fill": "#374151", "stroke": "#374151", "rx": 0, "ry": 0}}, "label": {{"text": "Sincronización", "fill": "#ffffff"}} }}
}}

- INICIO (START): type: "standard.Circle", nodeType: "START", size: {{"width": 80, "height": 80}}, attrs.body: {{"fill": "#d1fae5", "stroke": "#10b981"}}
- FIN (END): type: "standard.Circle", nodeType: "END", size: {{"width": 90, "height": 90}}, attrs.body: {{"fill": "#fee2e2", "stroke": "#ef4444", "strokeWidth": 4}}

REGLA 3: ENLACES Y DECISIONES OBLIGATORIAS
Todo enlace debe conectar "source": {{"id": "origen"}} con "target": {{"id": "destino"}}.
IMPORTANTE: Si la flecha sale de un nodo 'DECISION', DEBES obligatoriamente incluir la condición SpEL para el motor:
{{
  "type": "standard.Link", "id": "link_dec", "z": 0,
  "source": {{"id": "id_decision"}}, "target": {{"id": "id_destino"}},
  "conditionLabel": "Sí",
  "condition": {{ "type": "expression", "script": "#_decisionTomada == 'Sí'" }},
  "labels": [ {{ "position": 0.5, "attrs": {{ "text": {{"text": "Sí"}} }} }} ],
  "router": {{"name": "orthogonal", "args": {{"padding": 30}}}}, "connector": {{"name": "straight"}}
}}
Si no sale de una decisión, omite 'condition', 'conditionLabel' y 'labels'.

# ... (Reglas 1, 2 y 3 se mantienen idénticas) ...

REGLA 4: COORDENADAS DE NODOS Y METADATOS DE CARRILES (COLUMNAS VERTICALES)
¡Los carriles varían por empresa y son COLUMNAS VERTICALES! Lee el array 'lanes'.
1. METADATOS DE CARRILES: Llena el array "lanes_layout" en la raíz del JSON. Por cada carril activo, añade un objeto estricto con 'id', 'name', 'x' y 'width'. 
   Fórmula de Carril: El 'width' estándar es 600. El 'x' del carril es: (índice_del_carril * 600).
2. UBICACIÓN HORIZONTAL DE NODOS (X): Para que el nodo quede visualmente dentro de su carril, su posición X debe ser: (índice_del_carril * 600) + 200. Asigna SIEMPRE el "laneId" correcto a la tarea.
3. UBICACIÓN VERTICAL (Y): Incrementa la coordenada 'y' en 150 píxeles por cada nuevo paso en el flujo (el diagrama avanza de arriba hacia abajo). Si hay tareas en paralelo (FORK), ponlas en la misma coordenada 'y' pero en la 'x' de sus respectivos carriles.

Devuelve SOLO un JSON con esta estructura exacta:
{{
  "summary": "Resumen en texto",
  "changes": ["Cambio 1", "Cambio 2"],
  "warnings": [],
  "lanes": [
    {{ "id": "id_del_carril", "name": "Nombre Carril","color":"#EEF9F1", "x": 0, "width": 600 }},
    {{ "id": "otro_carril", "name": "Otro Carril", "color":"#EEF9F1", "x": 600, "width": 600 }}
  ],
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
        
        # --- NUEVA EXTRACCIÓN DE JSON BLINDADA ---
        start_idx = raw_text.find('{')
        end_idx = raw_text.rfind('}')
        
        if start_idx != -1 and end_idx != -1 and end_idx > start_idx:
            # Cortamos exactamente desde donde empieza hasta donde termina el objeto
            clean_json_str = raw_text[start_idx:end_idx+1]
        else:
            clean_json_str = raw_text

        try:
            parsed = json.loads(clean_json_str)
        except Exception as exc:
            # Si aún así falla, lanzamos un error claro para el log
            raise RuntimeError(f"Error parseando JSON: {exc}. Texto recibido: {clean_json_str}")

        if "diagram" not in parsed:
            raise RuntimeError("La IA no retornó la clave 'diagram'.")
            
        return parsed

    def _fallback_result(self, request: AgentRequest, base_diagram: Dict[str, Any]) -> AgentResult:
        # Extraemos los carriles originales enviados por el Frontend
        original_lanes = [lane.model_dump() for lane in request.lanes]
        
        diagram, warnings = sanitize_diagram(base_diagram, original_lanes)
        summary = "Modo fallback: Se mantuvo el diagrama original ante un error."
        
        return AgentResult(
            operation=request.operation,
            summary=summary,
            changes=["Saneamiento de seguridad aplicado."],
            warnings=warnings,
            diagram=diagram,
            lanes=original_lanes  # <--- Añadimos los carriles intactos para que no se borren en el frontend
        )
