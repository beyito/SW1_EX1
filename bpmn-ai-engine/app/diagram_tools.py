from __future__ import annotations

from copy import deepcopy
from typing import Any, Dict, List, Tuple
from uuid import uuid4

ALLOWED_NODE_TYPES = {"START", "TASK", "DECISION", "FORK", "JOIN", "SYNCHRONIZATION", "END"}
DEFAULT_NODE_SIZE = {"width": 140, "height": 70}
DEFAULT_POSITION = {"x": 100, "y": 100}

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
    node_lookup: Dict[str, str] = {}
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
            if cell_id:
                node_id = str(cell_id)
                node_ids.add(node_id)
                node_lookup[_normalize_ref(node_id)] = node_id

            label_text = ((cell.get("attrs") or {}).get("label") or {}).get("text")
            if isinstance(label_text, str) and label_text.strip() and cell_id:
                node_lookup[_normalize_ref(label_text)] = str(cell_id)
            
            # DEFENSIVA 1: Validar y forzar pertenencia a carriles
            if lane_ids:
                current_lane = cell.get("laneId")
                if not current_lane or current_lane not in lane_ids:
                    warnings.append(f"Corrigiendo laneId faltante o inválido para el nodo {cell_id}.")
                    cell["laneId"] = next(iter(lane_ids))
            
            # DEFENSIVA 2: Proteger formato de Size y Position
            if not isinstance(cell.get("size"), dict):
                cell["size"] = DEFAULT_NODE_SIZE
                warnings.append(f"Corrigiendo formato de tamaño en nodo {cell_id}.")
                
            if not isinstance(cell.get("position"), dict):
                cell["position"] = DEFAULT_POSITION
                warnings.append(f"Corrigiendo formato de posición en nodo {cell_id}.")
            
            filtered_cells.append(cell)
            continue
        
        filtered_cells.append(cell)

    # Segundo paso: Validar Enlaces (Links)
    valid_cells: List[Dict[str, Any]] = []
    for cell in filtered_cells:
        if cell.get("type") != "standard.Link":
            valid_cells.append(cell)
            continue

        # DEFENSIVA 3: Extraer origin/target incluso si la IA lo mandó como string
        source_val = cell.get("source")
        target_val = cell.get("target")

        raw_source_id = source_val if isinstance(source_val, str) else (source_val or {}).get("id", "")
        raw_target_id = target_val if isinstance(target_val, str) else (target_val or {}).get("id", "")

        source_id = str(raw_source_id).strip()
        target_id = str(raw_target_id).strip()

        resolved_source = _resolve_node_ref(source_id, node_ids, node_lookup)
        resolved_target = _resolve_node_ref(target_id, node_ids, node_lookup)
        
        if resolved_source and resolved_target:
            # Reconstruir como objeto estricto
            cell["source"] = {"id": resolved_source}
            cell["target"] = {"id": resolved_target}
            source_id = resolved_source
            target_id = resolved_target
        
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
        color = {"stroke": "#b45309", "fill": "#fbbf24", "refPoints": "0,60 60,0 120,60 60,120"} # FIX: refPoints agregado
    elif node_type == "FORK":
        shape = "standard.Rectangle"
        size = {"width": 20, "height": 160}
        color = {"stroke": "#1f2937", "fill": "#1f2937"}
    elif node_type in {"JOIN", "SYNCHRONIZATION"}:
        shape = "standard.Rectangle"
        size = {"width": 160, "height": 20}
        color = {"stroke": "#374151", "fill": "#374151"}
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

    if node_type in {"FORK", "JOIN", "SYNCHRONIZATION"}:
        node["attrs"]["label"]["fill"] = "#ffffff"
        node["attrs"]["label"]["fontWeight"] = "700"
        node["attrs"]["body"]["rx"] = 0
        node["attrs"]["body"]["ry"] = 0
    
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
                "targetMarker": {"type": "path", "d": "M 10 -5 0 0 10 5 z", "fill": "#0f172a", "stroke": "#0f172a", "stroke-width": 1}
            }
        }
    }

def _resolve_node_ref(raw_ref: str, node_ids: set[str], node_lookup: Dict[str, str]) -> str:
    if not raw_ref:
        return ""
    if raw_ref in node_ids:
        return raw_ref
    return node_lookup.get(_normalize_ref(raw_ref), "")

def _normalize_ref(value: str) -> str:
    return "".join(ch.lower() for ch in str(value) if ch.isalnum())