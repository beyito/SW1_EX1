from __future__ import annotations

from typing import Any, Dict

from fastapi import APIRouter, Request

from ..services import legacy_core


router = APIRouter(tags=["pln"])


@router.post("/api/v1/agent/policy-intent", response_model=legacy_core.PolicyIntentResponse)
def classify_policy_intent(
    payload: legacy_core.PolicyIntentRequest,
    http_request: Request,
) -> legacy_core.PolicyIntentResponse:
    return legacy_core.classify_policy_intent(payload, http_request)


@router.post("/api/v1/agent/voice-fill")
def voice_fill(
    payload: legacy_core.VoiceFillRequest,
    http_request: Request,
) -> Dict[str, Any]:
    return legacy_core.voice_fill(payload, http_request)
