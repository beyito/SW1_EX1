from __future__ import annotations

import logging
from copy import deepcopy
from typing import Any, Dict, List, Tuple
from uuid import uuid4

ALLOWED_NODE_TYPES = {"START", "TASK", "DECISION", "FORK", "JOIN", "SYNCHRONIZATION", "END"}
DEFAULT_NODE_SIZE = {"width": 140, "height": 70}
DEFAULT_POSITION = {"x": 100, "y": 100}
logger = logging.getLogger("bpmn_ai_sanitize")


def create_default_diagram() -> Dict[str, Any]:
    start_id = str(uuid4())
    task_id = str(uuid4())
    end_id = str(uuid4())

    cells = [
        _build_node(start_id, "START", "Inicio", 120, 180),
        _build_node(task_id, "TASK", "Tarea Inicial", 360, 170),
        _build_node(end_id, "END", "Fin", 620, 180),
        _build_link(start_id, task_id),
        _build_link(task_id, end_id),
    ]
    return {"cells": cells}


def sanitize_diagram(diagram: Dict[str, Any], lanes: List[Dict[str, Any]] | None = None) -> Tuple[Dict[str, Any], List[str]]:
    payload = deepcopy(diagram or {})
    warnings: List[str] = []
    cells = payload.get("cells")

    if not isinstance(cells, list):
        warnings.append("El diagrama no contenia celdas validas; se inicializo vacio.")
        cells = []

    filtered_cells: List[Dict[str, Any]] = []
    node_ids: set[str] = set()
    node_type_by_id: Dict[str, str] = {}
    node_lookup: Dict[str, str] = {}
    lane_ids = {lane.get("id") for lane in (lanes or []) if isinstance(lane, dict) and lane.get("id")}

    for cell in cells:
        if not isinstance(cell, dict):
            continue

        cell_type = str(cell.get("type", ""))
        node_type = str(cell.get("nodeType", "")).upper()

        if cell_type != "standard.Link":
            if not node_type:
                node_type = "TASK"
                cell["nodeType"] = "TASK"
                warnings.append(f"La IA omitio nodeType. Se asigno TASK al nodo {cell.get('id')}.")
            elif node_type not in ALLOWED_NODE_TYPES:
                warnings.append(f"Se elimino nodo de tipo no soportado: {node_type}.")
                continue

            cell_id = str(cell.get("id") or "").strip()
            if cell_id:
                node_ids.add(cell_id)
                node_type_by_id[cell_id] = node_type
                node_lookup[_normalize_ref(cell_id)] = cell_id

            label_text = ((cell.get("attrs") or {}).get("label") or {}).get("text")
            if isinstance(label_text, str) and label_text.strip() and cell_id:
                node_lookup[_normalize_ref(label_text)] = cell_id

            if lane_ids:
                current_lane = str(cell.get("laneId") or "").strip()
                if not current_lane or current_lane not in lane_ids:
                    cell["laneId"] = next(iter(lane_ids))
                    warnings.append(f"Se corrigio laneId invalido/faltante en nodo {cell_id}.")

            if not isinstance(cell.get("size"), dict):
                cell["size"] = deepcopy(DEFAULT_NODE_SIZE)
                warnings.append(f"Se corrigio size invalido en nodo {cell_id}.")
            if not isinstance(cell.get("position"), dict):
                cell["position"] = deepcopy(DEFAULT_POSITION)
                warnings.append(f"Se corrigio position invalido en nodo {cell_id}.")

            cell.pop("ports", None)
            _ensure_node_meta(cell)
            filtered_cells.append(cell)
            continue

        filtered_cells.append(cell)

    valid_cells: List[Dict[str, Any]] = []
    for cell in filtered_cells:
        if cell.get("type") != "standard.Link":
            valid_cells.append(cell)
            continue

        source_val = cell.get("source")
        target_val = cell.get("target")

        raw_source_id = source_val if isinstance(source_val, str) else (source_val or {}).get("id", "")
        raw_target_id = target_val if isinstance(target_val, str) else (target_val or {}).get("id", "")

        source_id = str(raw_source_id).strip()
        target_id = str(raw_target_id).strip()

        resolved_source = _resolve_node_ref(source_id, node_ids, node_lookup)
        resolved_target = _resolve_node_ref(target_id, node_ids, node_lookup)

        if resolved_source and resolved_target:
            cell["source"] = {"id": resolved_source}
            cell["target"] = {"id": resolved_target}
            source_id = resolved_source
            target_id = resolved_target

        _ensure_link_style(cell)
        _ensure_link_geometry(cell)
        _ensure_decision_link_condition(cell, node_type_by_id)

        if source_id in node_ids and target_id in node_ids:
            valid_cells.append(cell)
        else:
            warnings.append("Se elimino un enlace con origen o destino huerfano.")

    payload["cells"] = valid_cells
    _ensure_minimum_nodes(payload, warnings)
    logger.info(
        "sanitize_diagram.done input_cells=%s output_cells=%s lanes=%s warnings=%s",
        len(cells),
        len(valid_cells),
        len(lanes or []),
        len(warnings),
    )
    return payload, warnings


def _ensure_minimum_nodes(diagram: Dict[str, Any], warnings: List[str]) -> None:
    node_types = [str(cell.get("nodeType", "")).upper() for cell in diagram.get("cells", [])]
    if "START" not in node_types or "END" not in node_types:
        warnings.append("Faltan nodos criticos (START/END); el diagrama podria estar incompleto.")


def _build_node(node_id: str, node_type: str, label: str, x: int, y: int) -> Dict[str, Any]:
    if node_type in {"START", "END"}:
        shape = "standard.Circle"
        size = {"width": 80, "height": 80}
        color = {"stroke": "#10b981", "fill": "#d1fae5"} if node_type == "START" else {"stroke": "#ef4444", "fill": "#fee2e2"}
    elif node_type == "DECISION":
        shape = "standard.Polygon"
        size = {"width": 120, "height": 120}
        color = {"stroke": "#d97706", "fill": "#fffbeb", "refPoints": "0,60 60,0 120,60 60,120"}
    elif node_type == "FORK":
        shape = "standard.Rectangle"
        size = {"width": 20, "height": 160}
        color = {"stroke": "#1f2937", "fill": "#1f2937", "rx": 0, "ry": 0}
    elif node_type in {"JOIN", "SYNCHRONIZATION"}:
        shape = "standard.Rectangle"
        size = {"width": 160, "height": 20}
        color = {"stroke": "#374151", "fill": "#374151", "rx": 0, "ry": 0}
    else:
        shape = "standard.Rectangle"
        size = deepcopy(DEFAULT_NODE_SIZE)
        color = {"stroke": "#3b82f6", "fill": "#eff6ff", "rx": 8, "ry": 8}

    node = {
        "id": node_id,
        "type": shape,
        "nodeType": node_type,
        "position": {"x": x, "y": y},
        "size": size,
        "z": 10,
        "attrs": {
            "body": color,
            "label": {"text": label, "fill": "#1f2937", "fontWeight": "600"},
        },
    }

    _ensure_node_meta(node)
    return node


def _build_link(source_id: str, target_id: str) -> Dict[str, Any]:
    return {
        "id": str(uuid4()),
        "type": "standard.Link",
        "source": {"id": source_id},
        "target": {"id": target_id},
        "z": 0,
        "router": {"name": "orthogonal", "args": {"padding": 30}},
        "connector": {"name": "straight"},
        "attrs": {
            "line": {
                "stroke": "#0f172a",
                "strokeWidth": 3,
                "strokeLinecap": "round",
                "sourceMarker": None,
                "targetMarker": {"type": "path", "d": "M 10 -5 0 0 10 5 z", "fill": "#0f172a", "stroke": "#0f172a", "stroke-width": 1},
            }
        },
    }


def _ensure_node_meta(cell: Dict[str, Any]) -> None:
    node_type = str(cell.get("nodeType", "")).upper()
    node_meta = cell.get("nodeMeta")
    if not isinstance(node_meta, dict):
        node_meta = {}
        cell["nodeMeta"] = node_meta

    if node_type == "TASK":
        task_form = node_meta.get("taskForm")
        if not isinstance(task_form, dict):
            task_form = {}
            node_meta["taskForm"] = task_form
        task_form.setdefault("title", "")
        task_form.setdefault("description", "")
        task_form.setdefault("fields", [])
        task_form.setdefault("attachments", [])

    if node_type == "DECISION":
        node_meta.setdefault("decisionExpression", "")


def _ensure_link_style(cell: Dict[str, Any]) -> None:
    attrs = cell.get("attrs")
    if not isinstance(attrs, dict):
        attrs = {}
        cell["attrs"] = attrs

    line = attrs.get("line")
    if not isinstance(line, dict):
        line = {}
        attrs["line"] = line

    line.setdefault("stroke", "#0f172a")
    line.setdefault("strokeWidth", 3)
    line.setdefault("strokeLinecap", "round")
    line.setdefault("sourceMarker", None)
    if not isinstance(line.get("targetMarker"), dict):
        line["targetMarker"] = {
            "type": "path",
            "d": "M 10 -5 0 0 10 5 z",
            "fill": "#0f172a",
            "stroke": "#0f172a",
            "stroke-width": 1,
        }


def _ensure_link_geometry(cell: Dict[str, Any]) -> None:
    if not isinstance(cell.get("router"), dict):
        cell["router"] = {"name": "orthogonal", "args": {"padding": 30}}
    if not isinstance(cell.get("connector"), dict):
        cell["connector"] = {"name": "straight"}
    cell.setdefault("z", 0)


def _ensure_decision_link_condition(cell: Dict[str, Any], node_type_by_id: Dict[str, str]) -> None:
    source = cell.get("source")
    source_id = source.get("id") if isinstance(source, dict) else None
    if node_type_by_id.get(str(source_id)) != "DECISION":
        return

    label = str(cell.get("conditionLabel") or "").strip()
    if not label:
        label = "Si"
        cell["conditionLabel"] = label

    condition = cell.get("condition")
    if not isinstance(condition, dict):
        condition = {}
        cell["condition"] = condition
    condition["type"] = "expression"
    condition["script"] = f"#_decisionTomada == '{label}'"

    labels = cell.get("labels")
    if not isinstance(labels, list) or not labels:
        labels = [{"position": 0.5, "attrs": {"text": {"text": label}}}]
    else:
        first = labels[0] if isinstance(labels[0], dict) else {}
        attrs = first.get("attrs") if isinstance(first.get("attrs"), dict) else {}
        text = attrs.get("text") if isinstance(attrs.get("text"), dict) else {}
        text["text"] = label
        attrs["text"] = text
        first["attrs"] = attrs
        first.setdefault("position", 0.5)
        labels[0] = first
    cell["labels"] = labels


def _resolve_node_ref(raw_ref: str, node_ids: set[str], node_lookup: Dict[str, str]) -> str:
    if not raw_ref:
        return ""
    if raw_ref in node_ids:
        return raw_ref
    return node_lookup.get(_normalize_ref(raw_ref), "")


def _normalize_ref(value: str) -> str:
    return "".join(ch.lower() for ch in str(value) if ch.isalnum())
