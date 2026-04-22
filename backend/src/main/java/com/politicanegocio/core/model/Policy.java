package com.politicanegocio.core.model;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
@Document(collection = "policies")
public class Policy {
    @Id
    private String id;
    private String name;
    private String description;
    private String diagramJson; // Grafo serializado de JointJS
    private List<Lane> lanes = new ArrayList<>();
    private String companyId;
    private String companyName;
    private String createdBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
