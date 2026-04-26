import os
from dataclasses import dataclass
from functools import lru_cache
from typing import List

from dotenv import load_dotenv


load_dotenv()


@dataclass(frozen=True)
class Settings:
    app_name: str
    app_version: str
    host: str
    port: int
    reload: bool
    log_level: str
    allowed_origins: List[str]
    ai_api_key: str
    ai_model: str
    ai_models: List[str]
    ai_timeout_seconds: float
    ai_base_url: str


def _parse_allowed_origins(raw_value: str) -> List[str]:
    if not raw_value or not raw_value.strip():
        return ["*"]
    return [origin.strip() for origin in raw_value.split(",") if origin.strip()]


def _as_bool(raw_value: str, default: bool = False) -> bool:
    if raw_value is None:
        return default
    return raw_value.strip().lower() in {"1", "true", "yes", "on"}

def _parse_ai_models(raw_models: str, raw_single_model: str) -> List[str]:
    models: List[str] = []
    if raw_models and raw_models.strip():
        models.extend([item.strip() for item in raw_models.split(",") if item.strip()])
    if raw_single_model and raw_single_model.strip():
        models.append(raw_single_model.strip())

    if not models:
        models = ["gemini-3.1-flash-lite"]

    deduped: List[str] = []
    seen = set()
    for model in models:
        if model in seen:
            continue
        seen.add(model)
        deduped.append(model)
    return deduped


@lru_cache
def get_settings() -> Settings:
    ai_models = _parse_ai_models(
        os.getenv("AI_MODELS", ""),
        os.getenv("AI_MODEL", os.getenv("OPENAI_MODEL", "gemini-3.1-flash-lite")),
    )
    return Settings(
        app_name=os.getenv("AI_ENGINE_APP_NAME", "BPMN AI Engine"),
        app_version=os.getenv("AI_ENGINE_APP_VERSION", "1.0.0"),
        host=os.getenv("AI_ENGINE_HOST", "0.0.0.0"),
        port=int(os.getenv("AI_ENGINE_PORT", "8010")),
        reload=_as_bool(os.getenv("AI_ENGINE_RELOAD", "false")),
        log_level=os.getenv("AI_ENGINE_LOG_LEVEL", "INFO"),
        allowed_origins=_parse_allowed_origins(os.getenv("AI_ENGINE_ALLOWED_ORIGINS", "*")),
        ai_api_key=os.getenv("AI_API_KEY", os.getenv("OPENAI_API_KEY", "")).strip(),
        ai_model=ai_models[0],
        ai_models=ai_models,
        ai_timeout_seconds=float(os.getenv("AI_TIMEOUT_SECONDS", os.getenv("OPENAI_TIMEOUT_SECONDS", "45"))),
        ai_base_url=os.getenv("AI_BASE_URL", os.getenv("OPENAI_BASE_URL", "")).strip(),
    )
