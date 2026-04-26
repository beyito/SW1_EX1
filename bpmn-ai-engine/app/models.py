from __future__ import annotations

from typing import Any, Dict, List, Literal, Optional

from pydantic import BaseModel, Field


AllowedOperation = Literal["create", "modify"]


class LaneModel(BaseModel):
    id: str = Field(..., min_length=1)
    name: str = Field(..., min_length=1)
    color: str = Field(default="#dbeafe")
    x: Optional[float] = None


class AgentRequest(BaseModel):
    operation: AllowedOperation
    instruction: str = Field(..., min_length=3)
    current_diagram: Optional[Dict[str, Any]] = None
    lanes: List[LaneModel] = Field(default_factory=list)
    context: Dict[str, Any] = Field(default_factory=dict)
    models: List[str] = Field(default_factory=list)


class AgentResult(BaseModel):
    operation: AllowedOperation
    summary: str
    changes: List[str] = Field(default_factory=list)
    warnings: List[str] = Field(default_factory=list)
    diagram: Dict[str, Any]


class HealthResponse(BaseModel):
    status: str
    model: str
    available_models: List[str] = Field(default_factory=list)
    ai_provider_configured: bool
