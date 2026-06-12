from __future__ import annotations

from fastapi import APIRouter, Request

from ..services import legacy_core


router = APIRouter(tags=["deep-learning"])


@router.post("/api/v1/prediction/analyze", response_model=legacy_core.PredictiveAnalysisResponse)
def predictive_analysis(
    payload: legacy_core.PredictiveAnalysisRequest,
    http_request: Request,
) -> legacy_core.PredictiveAnalysisResponse:
    return legacy_core.predictive_analysis(payload, http_request)
