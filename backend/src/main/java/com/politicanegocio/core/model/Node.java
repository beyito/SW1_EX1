package com.politicanegocio.core.model;

import lombok.Data;

@Data
public class Node {
    private String id;        // Ej: "nodo-1"
    private String type;      // Ej: "TASK", "DECISION", "START"
    private String laneId;    // A qué calle/rol pertenece
    private String label;     // El texto que se lee en la cajita
    private String formSchema;// El JSON del formulario si es una tarea
    private Double x;         // Posición X en el canvas
    private Double y;         // Posición Y en el canvas
}
