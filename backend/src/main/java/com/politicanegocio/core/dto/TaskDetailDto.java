package com.politicanegocio.core.dto;

import com.politicanegocio.core.model.TaskInstanceStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TaskDetailDto {
    private String id;
    private String policyId;
    private String taskId;
    private String taskName;
    private String processName;
    private TaskInstanceStatus status;
    private String description;
    private String formSchema;
    private String formData;
    private String diagramJson;
}
