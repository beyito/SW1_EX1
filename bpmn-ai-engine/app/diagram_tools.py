from __future__ import annotations

from copy import deepcopy
from typing import Any, Dict, List, Tuple
from uuid import uuid4

ALLOWED_NODE_TYPES = {"START", "TASK", "DECISION", "FORK", "JOIN", "END"}
DEFAULT_NODE_SIZE = {"width": 140, "height": 70}

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
        warnings.append("El diagrama no contenía celdas válidas; se inicializó vacío.")
        cells = []

    filtered_cells: List[Dict[str, Any]] = []
    node_ids = set()
    lane_ids = {lane.get("id") for lane in (lanes or []) if isinstance(lane, dict) and lane.get("id")}

    # Primer paso: Procesar Nodos
    for cell in cells:
        if not isinstance(cell, dict): continue
        cell_type = str(cell.get("type", ""))
        node_type = str(cell.get("nodeType", "")).upper()

        if cell_type != "standard.Link":
            if node_type and node_type not in ALLOWED_NODE_TYPES:
                warnings.append(f"Se eliminó nodo de tipo no soportado: '{node_type}'.")
                continue
            
            cell_id = cell.get("id")
            if cell_id: node_ids.add(str(cell_id))
            
            # Validar pertenencia a carriles
            if lane_ids and cell.get("laneId") and cell.get("laneId") not in lane_ids:
                warnings.append(f"Corrigiendo laneId inválido para el nodo {cell_id}.")
                cell["laneId"] = next(iter(lane_ids))
            
            filtered_cells.append(cell)
            continue
        
        filtered_cells.append(cell)

    # Segundo paso: Validar Enlaces (Links)
    valid_cells: List[Dict[str, Any]] = []
    for cell in filtered_cells:
        if cell.get("type") != "standard.Link":
            valid_cells.append(cell)
            continue

        source_id = ((cell.get("source") or {}).get("id") or "").strip()
        target_id = ((cell.get("target") or {}).get("id") or "").strip()
        
        if source_id in node_ids and target_id in node_ids:
            valid_cells.append(cell)
        else:
            warnings.append("Se eliminó un enlace con origen o destino huérfano.")

    payload["cells"] = valid_cells
    _ensure_minimum_nodes(payload, warnings)
    return payload, warnings

def _ensure_minimum_nodes(diagram: Dict[str, Any], warnings: List[str]) -> None:
    cells = diagram.get("cells", [])
    node_types = [str(cell.get("nodeType", "")).upper() for cell in cells]
    if "START" not in node_types or "END" not in node_types:
        warnings.append("Faltan nodos críticos (START/END); el diagrama podría estar incompleto.")

def _build_node(node_id: str, node_type: str, label: str, x: int, y: int) -> Dict[str, Any]:
    # Configuración de formas según BPMN
    if node_type in {"START", "END"}:
        shape = "standard.Circle"
        size = {"width": 80, "height": 80}
        color = {"stroke": "#10b981", "fill": "#d1fae5"} if node_type == "START" else {"stroke": "#ef4444", "fill": "#fee2e2"}
    elif node_type == "DECISION":
        shape = "standard.Polygon"
        size = {"width": 120, "height": 120}
        color = {"stroke": "#b45309", "fill": "#fbbf24"}
    else:
        shape = "standard.Rectangle"
        size = DEFAULT_NODE_SIZE
        color = {"stroke": "#3b82f6", "fill": "#eff6ff"}

    node = {
        "id": node_id,
        "type": shape,
        "nodeType": node_type,
        "position": {"x": x, "y": y},
        "size": size,
        "z": 10,
        "attrs": {
            "body": {**color, "rx": 8, "ry": 8} if shape == "standard.Rectangle" else color,
            "label": {"text": label, "fill": "#1f2937", "fontWeight": "600"}
        },
        "ports": {
            "groups": {
                "in": {"position": {"name": "left"}, "attrs": {"portBody": {"magnet": True, "r": 8, "opacity": 0}}},
                "out": {"position": {"name": "right"}, "attrs": {"portBody": {"magnet": True, "r": 8, "opacity": 0}}}
            },
            "items": [{"id": "in", "group": "in"}, {"id": "out", "group": "out"}]
        }
    }
    
    if node_type == "TASK":
        node["nodeMeta"] = {"taskForm": {"title": "", "description": "", "fields": [], "attachments": []}}
    
    return node

def _build_link(source_id: str, target_id: str) -> Dict[str, Any]:
    return {
        "id": str(uuid4()),
        "type": "standard.Link",
        "source": {"id": source_id},
        "target": {"id": target_id},
        "z": 0,
        "router": {"name": "orthogonal", "args": {"padding": 30}},
        "connector": {"name": "straight", "args": {"cornerType": "line"}},
        "attrs": {
            "line": {
                "stroke": "#0f172a",
                "strokeWidth": 3,
                "targetMarker": {"fill": "#0f172a", "stroke": "#0f172a"}
            }
        }
    }