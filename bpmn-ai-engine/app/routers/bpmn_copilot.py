from __future__ import annotations

from fastapi import APIRouter, Request

from ..services import legacy_core
from ..models import AgentRequest, AgentResult


router = APIRouter(tags=["bpmn-copilot"])


@router.post("/api/v1/agent/diagram", response_model=AgentResult)
def process_diagram(request: AgentRequest) -> AgentResult:
    return legacy_core.process_diagram(request)


@router.post("/api/ai/copilot-chat", response_model=legacy_core.CopilotResponse)
def copilot_chat(
    payload: legacy_core.CopilotRequest,
    http_request: Request,
) -> legacy_core.CopilotResponse:
    return legacy_core.copilot_chat(payload, http_request)
