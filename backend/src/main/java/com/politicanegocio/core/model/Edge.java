package com.politicanegocio.core.model;

import lombok.Data;

@Data
public class Edge {
    private String id;           // Ej: "edge-1"
    private String sourceNodeId; // De dónde sale la flecha
    private String targetNodeId; // A dónde llega la flecha
    private String condition;    // Ej: "monto > 1000" (Para nodos de decisión)
    private Double manualX;
    private Double manualY;
    private Double offX1;
    private Double offY1;
    private Double offX2;
    private Double offY2;
}