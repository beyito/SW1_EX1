from __future__ import annotations

from fastapi import APIRouter

from ..services import legacy_core


router = APIRouter(tags=["health"])


@router.get("/health", response_model=legacy_core.HealthResponse)
def health() -> legacy_core.HealthResponse:
    return legacy_core.health()
