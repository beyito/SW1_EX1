from __future__ import annotations

from typing import Any, Callable, Dict, List

from ..local_ml.offline_models import OfflineDeepLearningService


class PredictiveOrchestrator:
    def __init__(self, offline_service: OfflineDeepLearningService) -> None:
        self.offline_service = offline_service

    def analyze(
        self,
        payload: Any,
        *,
        tensorflow_available: bool,
        autoencoder_anomalies: Callable[[Any], tuple[List[Dict[str, Any]], str]],
        document_burst_anomalies: Callable[[Any], List[Dict[str, Any]]],
        predict_priorities: Callable[[Any, Any], List[Dict[str, Any]]],
        predict_routes: Callable[[Any], List[Dict[str, Any]]],
        predict_bottlenecks: Callable[[Any], List[Dict[str, Any]]],
    ) -> Dict[str, Any]:
        task_anomalies, strategy = autoencoder_anomalies(payload.tasks)
        document_anomalies = document_burst_anomalies(payload.documents)
        legacy_priorities = predict_priorities(payload.processes, payload.tasks)
        legacy_routes = predict_routes(payload.tasks)
        legacy_bottlenecks = predict_bottlenecks(payload.tasks)
        offline_result = self.offline_service.analyze(payload)

        warnings: List[str] = []
        if not tensorflow_available:
            warnings.append("TensorFlow no está instalado en el motor IA; se usó cálculo heurístico.")
        if len(payload.tasks) < 8:
            warnings.append("Histórico insuficiente para entrenar autoencoder con buena confianza.")
        warnings.extend(offline_result.get("warnings", []))

        return {
            "model_strategy": f"{strategy}+{offline_result.get('strategy', 'offline_local')}",
            "anomalies": [
                *task_anomalies,
                *document_anomalies,
                *offline_result.get("document_anomalies", []),
            ],
            "priorities": _merge_by_key(
                legacy_priorities,
                offline_result.get("priorities", []),
                "processInstanceId",
            ),
            "route_predictions": _merge_by_key(
                legacy_routes,
                offline_result.get("route_predictions", []),
                "currentTaskId",
                scoped=True,
            ),
            "bottlenecks": _merge_by_key(
                legacy_bottlenecks,
                offline_result.get("bottlenecks", []),
                "laneId",
            ),
            "warnings": warnings,
        }


def _merge_by_key(
    primary: List[Dict[str, Any]],
    secondary: List[Dict[str, Any]],
    key: str,
    scoped: bool = False,
) -> List[Dict[str, Any]]:
    merged: Dict[str, Dict[str, Any]] = {}
    for item in [*secondary, *primary]:
        item_key = str(item.get(key) or "").strip()
        if not item_key and key == "currentTaskId":
            item_key = str(item.get("taskId") or "").strip()
            if item_key:
                item["currentTaskId"] = item_key
        if scoped:
            scope = str(item.get("policyId") or "").strip()
            item_key = f"{scope}:{item_key}"
        if not item_key:
            item_key = f"generated-{len(merged)}"
        merged[item_key] = item
    return list(merged.values())
