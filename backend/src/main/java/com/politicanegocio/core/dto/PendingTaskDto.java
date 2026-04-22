package com.politicanegocio.core.dto;

import com.politicanegocio.core.model.TaskInstanceStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PendingTaskDto {
    private String taskInstanceId;
    private String processInstanceId;
    private String policyId;
    private String processName;
    private String taskId;
    private String taskName;
    private String laneId;
    private TaskInstanceStatus status;
    private LocalDateTime createdAt;
}
