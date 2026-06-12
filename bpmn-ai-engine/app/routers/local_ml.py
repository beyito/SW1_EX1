from __future__ import annotations

from fastapi import APIRouter

from ..local_ml.datasets import load_document_activity_history, load_policy_intents, load_task_history
from ..local_ml.offline_models import LOCAL_MODEL_DIR, TENSORFLOW_LOCAL_AVAILABLE


router = APIRouter(prefix="/api/v1/local-ml", tags=["local-ml"])


@router.get("/status")
def local_ml_status() -> dict[str, object]:
    return {
        "offline": True,
        "tensorflow_available": TENSORFLOW_LOCAL_AVAILABLE,
        "datasets": {
            "predictive_task_history": len(load_task_history()),
            "document_activity_history": len(load_document_activity_history()),
            "policy_intents": len(load_policy_intents()),
        },
        "local_model_dir": str(LOCAL_MODEL_DIR),
        "model_artifacts": sorted(path.name for path in LOCAL_MODEL_DIR.glob("*")) if LOCAL_MODEL_DIR.exists() else [],
    }
