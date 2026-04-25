package com.politicanegocio.core.dto;

import com.politicanegocio.core.model.ProcessInstanceStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProcessTaskGroupDto {
    private String processInstanceId;
    private String policyId;
    private String processTitle;
    private String processDescription;
    private ProcessInstanceStatus processStatus;
    private LocalDateTime startedAt;
    private LocalDateTime completedAt;
    private List<ProcessTaskDto> tasks;
}
