from __future__ import annotations

import logging

from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware

from .config import get_settings
from .routers.analytics import router as analytics_router
from .routers.bpmn_copilot import router as bpmn_copilot_router
from .routers.deep_learning import router as deep_learning_router
from .routers.health import router as health_router
from .routers.local_ml import router as local_ml_router
from .routers.pln import router as pln_router


settings = get_settings()

logging.basicConfig(level=getattr(logging, settings.log_level.upper(), logging.INFO))

app = FastAPI(
    title=settings.app_name,
    version=settings.app_version,
    description="Motor IA modular para BPMN, PLN, Deep Learning predictivo y analítica.",
)

app.add_middleware(
    CORSMiddleware,
    allow_origins=settings.allowed_origins,
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

app.include_router(health_router)
app.include_router(pln_router)
app.include_router(deep_learning_router)
app.include_router(analytics_router)
app.include_router(bpmn_copilot_router)
app.include_router(local_ml_router)
