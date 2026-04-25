package com.politicanegocio.core.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PolicyTaskMetricDto {
    private String taskId;
    private String taskName;
    private long totalCompleted;
    private double avgWaitMinutes;
    private double avgWaitHours;
    private double avgExecutionMinutes;
    private double avgExecutionHours;
    private double avgTotalMinutes;
}
