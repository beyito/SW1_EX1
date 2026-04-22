package com.politicanegocio.core.model;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Data
@Document(collection = "process_instances")
public class ProcessInstance {
    @Id
    private String id;
    private String policyId;
    private ProcessInstanceStatus status;
    private String startedBy;
    private LocalDateTime startedAt;
    private LocalDateTime completedAt;
}
