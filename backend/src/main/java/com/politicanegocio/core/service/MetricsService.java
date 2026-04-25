package com.politicanegocio.core.service;

import com.politicanegocio.core.dto.PolicyTaskMetricDto;
import com.politicanegocio.core.model.ProcessInstance;
import com.politicanegocio.core.model.TaskInstance;
import com.politicanegocio.core.model.TaskInstanceStatus;
import com.politicanegocio.core.repository.ProcessInstanceRepository;
import com.politicanegocio.core.repository.TaskInstanceRepository;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class MetricsService {
    private final ProcessInstanceRepository processInstanceRepository;
    private final TaskInstanceRepository taskInstanceRepository;
    private final WorkflowEngine workflowEngine;

    public MetricsService(
            ProcessInstanceRepository processInstanceRepository,
            TaskInstanceRepository taskInstanceRepository,
            WorkflowEngine workflowEngine
    ) {
        this.processInstanceRepository = processInstanceRepository;
        this.taskInstanceRepository = taskInstanceRepository;
        this.workflowEngine = workflowEngine;
    }

    public List<PolicyTaskMetricDto> getPolicyMetrics(String policyId) {
        if (policyId == null || policyId.isBlank()) {
            return List.of();
        }

        List<String> processIds = processInstanceRepository.findByPolicyId(policyId).stream()
                .map(ProcessInstance::getId)
                .filter(id -> id != null && !id.isBlank())
                .toList();

        if (processIds.isEmpty()) {
            return List.of();
        }

        List<TaskInstance> completedTasks = taskInstanceRepository.findByProcessInstanceIdInAndStatus(
                processIds,
                TaskInstanceStatus.COMPLETED
        );
        if (completedTasks.isEmpty()) {
            return List.of();
        }

        Map<String, Aggregate> byTask = new LinkedHashMap<>();

        for (TaskInstance task : completedTasks) {
            String taskId = task.getTaskId() == null ? "" : task.getTaskId().trim();
            if (taskId.isBlank()) {
                continue;
            }

            Aggregate aggregate = byTask.computeIfAbsent(taskId, key -> new Aggregate());
            aggregate.totalCompleted += 1;

            LocalDateTime createdAt = task.getCreatedAt();
            LocalDateTime startedAt = task.getStartedAt();
            LocalDateTime completedAt = task.getCompletedAt();

            if (createdAt != null && startedAt != null && !startedAt.isBefore(createdAt)) {
                aggregate.waitMinutesTotal += Duration.between(createdAt, startedAt).toMinutes();
                aggregate.waitSamples += 1;
            }

            if (startedAt != null && completedAt != null && !completedAt.isBefore(startedAt)) {
                aggregate.executionMinutesTotal += Duration.between(startedAt, completedAt).toMinutes();
                aggregate.executionSamples += 1;
            }
        }

        return byTask.entrySet().stream()
                .map(entry -> toDto(policyId, entry.getKey(), entry.getValue()))
                .sorted((left, right) -> Double.compare(right.getAvgTotalMinutes(), left.getAvgTotalMinutes()))
                .toList();
    }

    private PolicyTaskMetricDto toDto(String policyId, String taskId, Aggregate aggregate) {
        double avgWaitMinutes = aggregate.waitSamples == 0 ? 0 : aggregate.waitMinutesTotal / aggregate.waitSamples;
        double avgExecutionMinutes = aggregate.executionSamples == 0
                ? 0
                : aggregate.executionMinutesTotal / aggregate.executionSamples;
        double avgTotalMinutes = avgWaitMinutes + avgExecutionMinutes;
        String taskName = workflowEngine.getNodeName(policyId, taskId);

        return new PolicyTaskMetricDto(
                taskId,
                taskName == null || taskName.isBlank() ? taskId : taskName,
                aggregate.totalCompleted,
                round2(avgWaitMinutes),
                round2(avgWaitMinutes / 60.0),
                round2(avgExecutionMinutes),
                round2(avgExecutionMinutes / 60.0),
                round2(avgTotalMinutes)
        );
    }

    private double round2(double value) {
        return Math.round(value * 100.0) / 100.0;
    }

    private static class Aggregate {
        private long totalCompleted;
        private double waitMinutesTotal;
        private long waitSamples;
        private double executionMinutesTotal;
        private long executionSamples;
    }
}
