package com.politicanegocio.core.model;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Data
@Document(collection = "task_instances")
public class TaskInstance {
    @Id
    private String id;
    private String processInstanceId;
    private String taskId;
    private String laneId;
    private TaskInstanceStatus status;
    private String assignedTo;
    private String formData;
    private LocalDateTime createdAt;
    private LocalDateTime startedAt;
    private LocalDateTime completedAt;
}
