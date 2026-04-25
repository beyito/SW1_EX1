package com.politicanegocio.core.dto;

import com.politicanegocio.core.model.TaskInstanceStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProcessTaskDto {
    private String taskInstanceId;
    private String taskId;
    private String taskName;
    private String laneId;
    private TaskInstanceStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime startedAt;
    private LocalDateTime completedAt;
}
