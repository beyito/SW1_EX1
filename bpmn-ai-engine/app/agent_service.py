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

    def process(self, request: AgentRequest) -> AgentResult:
        if request.operation == "create":
            base_diagram = create_default_diagram()
        else:
            base_diagram = deepcopy(request.current_diagram or create_default_diagram())

        if self.client is None:
            return self._fallback_result(request, base_diagram)

        try:
            ai_result = self._run_llm(request, base_diagram)
            
            normalized_diagram, sanitize_warnings = sanitize_diagram(
                ai_result.get("diagram") or base_diagram,
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

    def _run_llm(self, request: AgentRequest, base_diagram: Dict[str, Any]) -> Dict[str, Any]:
        system_prompt = f"""Eres el motor de IA de un modelador BPMN en Angular usando JointJS.
Genera o modifica diagramas en formato JSON estricto.

REGLA 1: PRESERVACIÓN ABSOLUTA DEL ESTADO
Si un nodo o enlace ya existe en 'current_diagram' (por ID), NO alteres su 'type', 'attrs', 'size', 'ports' ni 'nodeMeta'. Solo cambia 'position', 'laneId' o agrega información si el usuario te lo pide explícitamente (ej: agregar campos a un formulario).

REGLA 2: FORMATO EXACTO PARA NUEVOS NODOS
Si creas nodos nuevos, deben incluir 'id', 'nodeType', 'laneId', 'position' (alineado con la 'x' de su lane), 'z': 10, y 'ports' magnéticos (in: left, out: right con opacity: 0).
Estilos estrictos según el nodeType:
- START: type 'standard.Circle', size 80x80, attrs.body (fill '#d1fae5', stroke '#10b981').
- END: type 'standard.Circle', size 90x90, attrs.body (fill '#fee2e2', stroke '#ef4444', strokeWidth 4).
- TASK: type 'standard.Rectangle', size 140x70, attrs.body (fill '#eff6ff', stroke '#3b82f6', rx 8, ry 8). IMPORTANTE: Incluir 'nodeMeta': {{"taskForm": {{"title": "", "description": "", "fields": [], "attachments": []}}}}.
- DECISION: type 'standard.Polygon', size 120x120, attrs.body (refPoints '0,60 60,0 120,60 60,120', fill '#fbbf24', stroke '#b45309'). Incluir 'nodeMeta': {{"decisionExpression": ""}}.
- FORK: type 'standard.Rectangle', size 20x160, attrs.body (fill '#000000', stroke '#000000', rx 0, ry 0).
- JOIN / SYNCHRONIZATION: type 'standard.Rectangle', size 160x20, attrs.body (fill '#374151', stroke '#374151', rx 0, ry 0).

REGLA 3: FORMATO EXACTO PARA NUEVOS ENLACES
- type: 'standard.Link', z: 0.
- router: {{"name": "orthogonal", "args": {{"padding": 30}}}}.
- connector: {{"name": "straight", "args": {{"cornerType": "line"}}}}.
- attrs.line: {{"stroke": "#0f172a", "strokeWidth": 3, "targetMarker": {{"type": "path", "d": "M 10 -5 0 0 10 5 z", "fill": "#0f172a", "stroke": "#0f172a", "stroke-width": 1}}}}.
Si sale de un DECISION, agrega 'conditionLabel' y un label en position 0.5.

Devuelve SOLO un JSON válido con esta estructura exacta:
{{
  "summary": "Resumen en texto",
  "changes": ["Cambio 1 en texto", "Cambio 2 en texto"],
  "warnings": ["Advertencia 1 en texto"],
  "diagram": {{ "cells": [...] }}
}}
IMPORTANTE: Las claves 'changes' y 'warnings' DEBEN ser listas de STRINGS, NO objetos JSON."""

        user_payload = {
            "operation": request.operation,
            "instruction": request.instruction,
            "lanes": [lane.model_dump() for lane in request.lanes],
            "current_diagram": base_diagram,
        }

        response = self.client.chat.completions.create(
            model=self.settings.ai_model,
            temperature=0.1,
            response_format={ "type": "json_object" },
            messages=[
                {"role": "system", "content": system_prompt},
                {"role": "user", "content": json.dumps(user_payload, ensure_ascii=True)},
            ],
        )
        
        raw_text = (response.choices[0].message.content or "").strip()
        
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