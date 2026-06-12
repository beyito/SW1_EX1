from __future__ import annotations

import json
import math
from collections import Counter, defaultdict
from datetime import datetime
from pathlib import Path
from typing import Any, Dict, Iterable, List

from .datasets import load_document_activity_history, load_task_history

try:
    import numpy as np
    import tensorflow as tf
    from tensorflow import keras

    TENSORFLOW_LOCAL_AVAILABLE = True
except Exception:  # pragma: no cover - depends on runtime image
    np = None
    tf = None
    keras = None
    TENSORFLOW_LOCAL_AVAILABLE = False


PRIORITY_LABELS = ["BAJA", "MEDIA", "ALTA", "CRITICA"]
LOCAL_MODEL_DIR = Path(__file__).resolve().parents[2] / "models" / "local"


class OfflineDeepLearningService:
    """Small local models trained from repo datasets plus runtime event log.

    The service is intentionally self-contained: it does not call external APIs,
    does not require internet, and falls back to deterministic heuristics when
    TensorFlow is unavailable in the host environment.
    """

    def analyze(self, payload: Any) -> Dict[str, Any]:
        processes = [_to_dict(item) for item in getattr(payload, "processes", [])]
        tasks = [_to_dict(item) for item in getattr(payload, "tasks", [])]
        documents = [_to_dict(item) for item in getattr(payload, "documents", [])]

        historical_tasks = load_task_history()
        historical_documents = load_document_activity_history()

        priorities = self.predict_priorities(processes, tasks, historical_tasks)
        routes = self.predict_routes(tasks, historical_tasks)
        bottlenecks = self.predict_bottlenecks(tasks, historical_tasks)
        document_anomalies = self.detect_document_bursts(documents, historical_documents)

        return {
            "strategy": "local_tensorflow_offline" if TENSORFLOW_LOCAL_AVAILABLE else "local_heuristic_offline",
            "priorities": priorities,
            "route_predictions": routes,
            "bottlenecks": bottlenecks,
            "document_anomalies": document_anomalies,
            "warnings": self._warnings(historical_tasks),
        }

    def predict_priorities(
        self,
        processes: List[Dict[str, Any]],
        tasks: List[Dict[str, Any]],
        historical_tasks: List[Dict[str, Any]],
    ) -> List[Dict[str, Any]]:
        task_counts = Counter(_clean(item.get("processInstanceId")) for item in tasks)
        open_counts = Counter(
            _clean(item.get("processInstanceId"))
            for item in tasks
            if _clean(item.get("status")).upper() in {"PENDING", "IN_PROGRESS"}
        )
        durations_by_process: Dict[str, List[float]] = defaultdict(list)
        for task in tasks:
            duration = _minutes_between(task.get("startedAt"), task.get("completedAt"))
            if duration > 0:
                durations_by_process[_clean(task.get("processInstanceId"))].append(duration)

        trained = self._train_priority_model(historical_tasks)
        rows: List[Dict[str, Any]] = []
        for process in processes:
            process_id = _clean(process.get("id"))
            avg_duration = _avg(durations_by_process.get(process_id, []))
            feature = [
                float(task_counts.get(process_id, 0)),
                float(open_counts.get(process_id, 0)),
                avg_duration,
                _status_score(process.get("status")),
            ]
            label, confidence = self._predict_priority_label(feature, trained)
            rows.append(
                {
                    "processInstanceId": process_id,
                    "policyId": _clean(process.get("policyId")),
                    "priority": label,
                    "confidence": round(confidence, 3),
                    "reason": "Modelo local offline evaluó carga, estado y duración promedio.",
                }
            )
        return rows

    def predict_routes(self, tasks: List[Dict[str, Any]], historical_tasks: List[Dict[str, Any]]) -> List[Dict[str, Any]]:
        transitions: Counter[tuple[str, str]] = Counter()
        contextual_transitions: Dict[tuple[str, str], Counter[str]] = defaultdict(Counter)
        global_transitions: Dict[str, Counter[str]] = defaultdict(Counter)
        grouped = _group_by_process([*_normalize_historical_tasks(historical_tasks), *tasks])
        for process_tasks in grouped.values():
            ordered = sorted(process_tasks, key=lambda item: _parse_datetime(item.get("createdAt")) or datetime.min)
            for current, nxt in zip(ordered, ordered[1:]):
                policy_id = _clean(current.get("policyId")) or _clean(nxt.get("policyId"))
                current_id = _clean(current.get("taskId"))
                next_id = _clean(nxt.get("taskId"))
                if current_id and next_id:
                    transitions[(current_id, next_id)] += 1
                    contextual_transitions[(policy_id, current_id)][next_id] += 1
                    global_transitions[current_id][next_id] += 1

        route_model = self._train_route_model(transitions)
        predictions = []
        seen = set()
        for task in tasks:
            task_id = _clean(task.get("taskId"))
            policy_id = _clean(task.get("policyId"))
            route_key = (policy_id, task_id)
            if not task_id or route_key in seen:
                continue
            seen.add(route_key)

            contextual_counts = contextual_transitions.get(route_key)
            if contextual_counts:
                next_id, count = contextual_counts.most_common(1)[0]
                total = sum(contextual_counts.values())
                predictions.append(
                    {
                        "policyId": policy_id,
                        "currentTaskId": task_id,
                        "taskId": task_id,
                        "predictedNextTaskId": next_id,
                        "probability": round(count / max(total, 1), 3),
                        "support": count,
                        "totalSimilarCases": total,
                        "reason": "Modelo local comparó la tarea actual con rutas históricas del mismo trámite.",
                    }
                )
                continue

            global_counts = global_transitions.get(task_id)
            if global_counts:
                next_id, count = global_counts.most_common(1)[0]
                total = sum(global_counts.values())
                predictions.append(
                    {
                        "policyId": policy_id,
                        "currentTaskId": task_id,
                        "taskId": task_id,
                        "predictedNextTaskId": next_id,
                        "probability": round(count / max(total, 1), 3),
                        "support": count,
                        "totalSimilarCases": total,
                        "reason": "Modelo local usó transiciones históricas globales para esta tarea.",
                    }
                )
                continue

            neural_prediction = self._predict_route_with_model(task_id, route_model)
            if neural_prediction:
                next_id, probability = neural_prediction
                predictions.append(
                    {
                        "policyId": policy_id,
                        "currentTaskId": task_id,
                        "taskId": task_id,
                        "predictedNextTaskId": next_id,
                        "probability": round(probability, 3),
                        "support": 0,
                        "totalSimilarCases": 0,
                        "reason": "Clasificador local Keras predijo la siguiente tarea sin internet.",
                    }
                )
        return sorted(predictions, key=lambda item: item.get("probability", 0), reverse=True)[:20]

    def _train_route_model(self, transitions: Counter[tuple[str, str]]) -> Dict[str, Any]:
        if not TENSORFLOW_LOCAL_AVAILABLE or np is None or keras is None or len(transitions) < 3:
            return {"enabled": False}

        task_ids = sorted({task_id for pair in transitions for task_id in pair if task_id})
        if len(task_ids) < 2:
            return {"enabled": False}
        index_by_task = {task_id: index for index, task_id in enumerate(task_ids)}
        loaded = self._load_route_model(task_ids)
        if loaded.get("enabled"):
            return loaded

        x_rows = []
        y_rows = []
        for (current_id, next_id), count in transitions.items():
            for _ in range(max(1, int(count))):
                row = [0.0] * len(task_ids)
                row[index_by_task[current_id]] = 1.0
                x_rows.append(row)
                y_rows.append(index_by_task[next_id])

        if len(set(y_rows)) < 2:
            return {"enabled": False}

        x = np.array(x_rows, dtype="float32")
        y = np.array(y_rows, dtype="int32")
        model = keras.Sequential(
            [
                keras.layers.Input(shape=(x.shape[1],)),
                keras.layers.Dense(12, activation="relu"),
                keras.layers.Dense(12, activation="relu"),
                keras.layers.Dense(len(task_ids), activation="softmax"),
            ]
        )
        model.compile(optimizer="adam", loss="sparse_categorical_crossentropy")
        model.fit(x, y, epochs=35, verbose=0)
        self._save_model(model, "route_classifier.keras", "route_classifier.json", {"task_ids": task_ids})
        return {"enabled": True, "model": model, "task_ids": task_ids, "index_by_task": index_by_task}

    def _predict_route_with_model(self, task_id: str, route_model: Dict[str, Any]) -> tuple[str, float] | None:
        if not route_model.get("enabled") or np is None:
            return None
        index_by_task = route_model["index_by_task"]
        if task_id not in index_by_task:
            return None
        task_ids = route_model["task_ids"]
        row = [0.0] * len(task_ids)
        row[index_by_task[task_id]] = 1.0
        probabilities = route_model["model"].predict(np.array([row], dtype="float32"), verbose=0)[0]
        next_index = int(probabilities.argmax())
        return task_ids[next_index], float(probabilities[next_index])

    def predict_bottlenecks(self, tasks: List[Dict[str, Any]], historical_tasks: List[Dict[str, Any]]) -> List[Dict[str, Any]]:
        lane_durations: Dict[str, List[float]] = defaultdict(list)
        for task in [*_normalize_historical_tasks(historical_tasks), *tasks]:
            lane_id = _clean(task.get("laneId"))
            duration = _minutes_between(task.get("startedAt"), task.get("completedAt"))
            if lane_id and duration > 0:
                lane_durations[lane_id].append(duration)

        bottleneck_model = self._train_bottleneck_model(lane_durations)
        rows = []
        for lane_id, durations in lane_durations.items():
            avg_duration = _avg(durations)
            pending = sum(
                1
                for task in tasks
                if _clean(task.get("laneId")) == lane_id
                and _clean(task.get("status")).upper() in {"PENDING", "IN_PROGRESS"}
            )
            risk = self._predict_bottleneck_risk(avg_duration, pending, bottleneck_model)
            if risk >= 0.35:
                rows.append(
                    {
                        "laneId": lane_id,
                        "risk": round(risk, 3),
                        "avgDurationMinutes": round(avg_duration, 2),
                        "pendingTasks": pending,
                        "reason": "Regresor local Keras estimó riesgo de cuello de botella sin internet."
                        if bottleneck_model.get("enabled")
                        else "Pronóstico local combina duración histórica y carga pendiente.",
                    }
                )
        return sorted(rows, key=lambda item: item["risk"], reverse=True)[:8]

    def _train_bottleneck_model(self, lane_durations: Dict[str, List[float]]) -> Dict[str, Any]:
        if not TENSORFLOW_LOCAL_AVAILABLE or np is None or keras is None or len(lane_durations) < 2:
            return {"enabled": False}
        loaded = self._load_bottleneck_model()
        if loaded.get("enabled"):
            return loaded

        features = []
        labels = []
        for durations in lane_durations.values():
            avg_duration = _avg(durations)
            samples = max(1, len(durations))
            synthetic_pending = min(8, samples)
            risk = min(0.99, (avg_duration / 240.0) + (synthetic_pending * 0.08))
            features.append([avg_duration, float(synthetic_pending)])
            labels.append(risk)

        x = np.array(features, dtype="float32")
        y = np.array(labels, dtype="float32")
        mean = x.mean(axis=0)
        std = x.std(axis=0)
        std[std == 0] = 1.0
        x_norm = (x - mean) / std

        model = keras.Sequential(
            [
                keras.layers.Input(shape=(2,)),
                keras.layers.Dense(8, activation="relu"),
                keras.layers.Dense(4, activation="relu"),
                keras.layers.Dense(1, activation="sigmoid"),
            ]
        )
        model.compile(optimizer="adam", loss="mse")
        model.fit(x_norm, y, epochs=35, verbose=0)
        self._save_model(
            model,
            "bottleneck_regressor.keras",
            "bottleneck_regressor.json",
            {"mean": mean.tolist(), "std": std.tolist()},
        )
        return {"enabled": True, "model": model, "mean": mean, "std": std}

    def _predict_bottleneck_risk(self, avg_duration: float, pending: int, model_info: Dict[str, Any]) -> float:
        if model_info.get("enabled") and np is not None:
            x = np.array([[avg_duration, float(pending)]], dtype="float32")
            x_norm = (x - model_info["mean"]) / model_info["std"]
            return min(0.99, max(0.0, float(model_info["model"].predict(x_norm, verbose=0)[0][0])))
        return min(0.99, (avg_duration / 240.0) + (pending * 0.08))

    def detect_document_bursts(
        self,
        documents: List[Dict[str, Any]],
        historical_documents: List[Dict[str, Any]],
    ) -> List[Dict[str, Any]]:
        threshold = 8
        historical_counts = [
            _float(item.get("count_last_minute"))
            for item in historical_documents
            if _float(item.get("count_last_minute")) > 0
        ]
        if historical_counts:
            threshold = max(8, math.ceil(_avg(historical_counts) + 2 * _std(historical_counts)))

        grouped: Dict[str, List[Dict[str, Any]]] = defaultdict(list)
        for document in documents:
            grouped[_clean(document.get("processInstanceId"))].append(document)

        anomalies = []
        for process_id, process_docs in grouped.items():
            if len(process_docs) >= threshold:
                anomalies.append(
                    {
                        "type": "DOCUMENT_BURST",
                        "processInstanceId": process_id,
                        "score": min(0.99, len(process_docs) / max(threshold, 1)),
                        "reason": f"Carga documental local supera el umbral histórico de {threshold} documentos.",
                    }
                )
        return anomalies

    def _train_priority_model(self, historical_tasks: List[Dict[str, Any]]) -> Dict[str, Any]:
        if not TENSORFLOW_LOCAL_AVAILABLE or np is None or keras is None:
            return {"enabled": False}
        loaded = self._load_priority_model()
        if loaded.get("enabled"):
            return loaded

        grouped = _group_by_process(_normalize_historical_tasks(historical_tasks))
        features = []
        labels = []
        for process_id, process_tasks in grouped.items():
            durations = [
                _minutes_between(task.get("startedAt"), task.get("completedAt"))
                for task in process_tasks
                if _minutes_between(task.get("startedAt"), task.get("completedAt")) > 0
            ]
            label = _clean(process_tasks[0].get("priority_label")).upper() or "MEDIA"
            if label not in PRIORITY_LABELS:
                label = "MEDIA"
            features.append([
                float(len(process_tasks)),
                float(sum(1 for task in process_tasks if _clean(task.get("status")).upper() != "COMPLETED")),
                _avg(durations),
                _status_score(process_tasks[-1].get("status")),
            ])
            labels.append(PRIORITY_LABELS.index(label))

        if len(features) < 4:
            return {"enabled": False}

        x = np.array(features, dtype="float32")
        y = np.array(labels, dtype="int32")
        mean = x.mean(axis=0)
        std = x.std(axis=0)
        std[std == 0] = 1.0
        x_norm = (x - mean) / std

        model = keras.Sequential(
            [
                keras.layers.Input(shape=(x_norm.shape[1],)),
                keras.layers.Dense(8, activation="relu"),
                keras.layers.Dense(8, activation="relu"),
                keras.layers.Dense(len(PRIORITY_LABELS), activation="softmax"),
            ]
        )
        model.compile(optimizer="adam", loss="sparse_categorical_crossentropy")
        model.fit(x_norm, y, epochs=25, verbose=0)
        self._save_model(
            model,
            "priority_classifier.keras",
            "priority_classifier.json",
            {"mean": mean.tolist(), "std": std.tolist()},
        )
        return {"enabled": True, "model": model, "mean": mean, "std": std}

    def _predict_priority_label(self, feature: List[float], trained: Dict[str, Any]) -> tuple[str, float]:
        if trained.get("enabled") and np is not None:
            x = np.array([feature], dtype="float32")
            x_norm = (x - trained["mean"]) / trained["std"]
            probabilities = trained["model"].predict(x_norm, verbose=0)[0]
            index = int(probabilities.argmax())
            return PRIORITY_LABELS[index], float(probabilities[index])

        score = min(1.0, (feature[1] * 0.2) + (feature[2] / 480.0) + (feature[3] * 0.2))
        if score >= 0.85:
            return "CRITICA", score
        if score >= 0.6:
            return "ALTA", score
        if score >= 0.3:
            return "MEDIA", score
        return "BAJA", max(score, 0.1)

    def _warnings(self, historical_tasks: List[Dict[str, Any]]) -> List[str]:
        warnings = ["Predicción enriquecida con datasets y modelos locales del repositorio."]
        if not TENSORFLOW_LOCAL_AVAILABLE:
            warnings.append("TensorFlow no está disponible; se usaron modelos heurísticos locales.")
        if len(historical_tasks) < 12:
            warnings.append("Dataset local pequeño; se recomienda ampliarlo con histórico real exportado desde Spring Boot.")
        return warnings

    def _load_priority_model(self) -> Dict[str, Any]:
        metadata = self._load_metadata("priority_classifier.json")
        model = self._load_model("priority_classifier.keras")
        if not metadata or model is None or np is None:
            return {"enabled": False}
        return {
            "enabled": True,
            "model": model,
            "mean": np.array(metadata.get("mean", [0, 0, 0, 0]), dtype="float32"),
            "std": np.array(metadata.get("std", [1, 1, 1, 1]), dtype="float32"),
        }

    def _load_route_model(self, task_ids: List[str]) -> Dict[str, Any]:
        metadata = self._load_metadata("route_classifier.json")
        model = self._load_model("route_classifier.keras")
        saved_task_ids = metadata.get("task_ids") if metadata else None
        if not metadata or model is None or saved_task_ids != task_ids:
            return {"enabled": False}
        return {
            "enabled": True,
            "model": model,
            "task_ids": task_ids,
            "index_by_task": {task_id: index for index, task_id in enumerate(task_ids)},
        }

    def _load_bottleneck_model(self) -> Dict[str, Any]:
        metadata = self._load_metadata("bottleneck_regressor.json")
        model = self._load_model("bottleneck_regressor.keras")
        if not metadata or model is None or np is None:
            return {"enabled": False}
        return {
            "enabled": True,
            "model": model,
            "mean": np.array(metadata.get("mean", [0, 0]), dtype="float32"),
            "std": np.array(metadata.get("std", [1, 1]), dtype="float32"),
        }

    def _save_model(self, model: Any, model_name: str, metadata_name: str, metadata: Dict[str, Any]) -> None:
        try:
            LOCAL_MODEL_DIR.mkdir(parents=True, exist_ok=True)
            model.save(LOCAL_MODEL_DIR / model_name)
            (LOCAL_MODEL_DIR / metadata_name).write_text(
                json.dumps(metadata, ensure_ascii=False, indent=2),
                encoding="utf-8",
            )
        except Exception:
            return

    def _load_model(self, model_name: str) -> Any | None:
        if keras is None:
            return None
        path = LOCAL_MODEL_DIR / model_name
        if not path.exists():
            return None
        try:
            return keras.models.load_model(path)
        except Exception:
            return None

    def _load_metadata(self, metadata_name: str) -> Dict[str, Any] | None:
        path = LOCAL_MODEL_DIR / metadata_name
        if not path.exists():
            return None
        try:
            return json.loads(path.read_text(encoding="utf-8"))
        except Exception:
            return None


def _to_dict(item: Any) -> Dict[str, Any]:
    if isinstance(item, dict):
        return item
    if hasattr(item, "model_dump"):
        return item.model_dump()
    if hasattr(item, "dict"):
        return item.dict()
    return {}


def _normalize_historical_tasks(rows: Iterable[Dict[str, Any]]) -> List[Dict[str, Any]]:
    normalized = []
    for row in rows:
        normalized.append(
            {
                "processInstanceId": row.get("process_instance_id"),
                "policyId": row.get("policy_id"),
                "taskId": row.get("task_id"),
                "laneId": row.get("lane_id"),
                "status": row.get("status"),
                "createdAt": row.get("created_at"),
                "startedAt": row.get("started_at"),
                "completedAt": row.get("completed_at"),
                "priority_label": row.get("priority_label"),
            }
        )
    return normalized


def _group_by_process(tasks: Iterable[Dict[str, Any]]) -> Dict[str, List[Dict[str, Any]]]:
    grouped: Dict[str, List[Dict[str, Any]]] = defaultdict(list)
    for task in tasks:
        process_id = _clean(task.get("processInstanceId"))
        if process_id:
            grouped[process_id].append(task)
    return grouped


def _parse_datetime(value: Any) -> datetime | None:
    if value is None:
        return None
    raw = str(value).strip()
    if not raw:
        return None
    try:
        return datetime.fromisoformat(raw.replace("Z", "+00:00"))
    except ValueError:
        return None


def _minutes_between(start: Any, end: Any) -> float:
    start_dt = _parse_datetime(start)
    end_dt = _parse_datetime(end)
    if not start_dt or not end_dt:
        return 0.0
    return max((end_dt - start_dt).total_seconds() / 60.0, 0.0)


def _status_score(status: Any) -> float:
    normalized = _clean(status).upper()
    return {
        "COMPLETED": 0.1,
        "ACTIVE": 0.35,
        "PENDING": 0.45,
        "IN_PROGRESS": 0.6,
        "REJECTED": 0.8,
        "CANCELLED": 0.7,
    }.get(normalized, 0.35)


def _avg(values: Iterable[float]) -> float:
    items = [value for value in values if value is not None]
    return sum(items) / len(items) if items else 0.0


def _std(values: Iterable[float]) -> float:
    items = [value for value in values if value is not None]
    if len(items) < 2:
        return 0.0
    avg = _avg(items)
    return math.sqrt(sum((value - avg) ** 2 for value in items) / len(items))


def _float(value: Any) -> float:
    try:
        return float(value)
    except (TypeError, ValueError):
        return 0.0


def _clean(value: Any) -> str:
    return str(value or "").strip()
