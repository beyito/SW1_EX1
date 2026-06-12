from __future__ import annotations

import csv
import json
from functools import lru_cache
from pathlib import Path
from typing import Any, Dict, List


DATASET_DIR = Path(__file__).resolve().parents[2] / "datasets"


@lru_cache
def load_task_history() -> List[Dict[str, Any]]:
    return _read_csv(DATASET_DIR / "predictive_task_history.csv")


@lru_cache
def load_document_activity_history() -> List[Dict[str, Any]]:
    return _read_csv(DATASET_DIR / "document_activity_history.csv")


@lru_cache
def load_policy_intents() -> List[Dict[str, Any]]:
    path = DATASET_DIR / "policy_intents.json"
    if not path.exists():
        return []
    return json.loads(path.read_text(encoding="utf-8"))


def _read_csv(path: Path) -> List[Dict[str, Any]]:
    if not path.exists():
        return []
    with path.open("r", encoding="utf-8", newline="") as file:
        return [dict(row) for row in csv.DictReader(file)]
