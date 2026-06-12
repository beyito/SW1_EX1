from __future__ import annotations

from fastapi import APIRouter, Request

from ..services import legacy_core


router = APIRouter(tags=["analytics"])


@router.post("/api/v1/analytics/report-plan", response_model=legacy_core.DynamicReportPlanResponse)
def dynamic_report_plan(
    payload: legacy_core.DynamicReportRequest,
    http_request: Request,
) -> legacy_core.DynamicReportPlanResponse:
    return legacy_core.dynamic_report_plan(payload, http_request)
