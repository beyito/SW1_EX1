package com.politicanegocio.core.controller;

import com.politicanegocio.core.dto.PendingTaskDto;
import com.politicanegocio.core.dto.ProcessTaskGroupDto;
import com.politicanegocio.core.dto.StartablePolicyDto;
import com.politicanegocio.core.dto.TaskDetailDto;
import com.politicanegocio.core.model.Policy;
import com.politicanegocio.core.model.ProcessInstance;
import com.politicanegocio.core.model.TaskInstance;
import com.politicanegocio.core.model.User;
import com.politicanegocio.core.service.PolicyService;
import com.politicanegocio.core.service.ProcessExecutionService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/execution")
public class ProcessExecutionController {

    private final ProcessExecutionService processExecutionService;
    private final PolicyService policyService;

    public ProcessExecutionController(ProcessExecutionService processExecutionService, PolicyService policyService) {
        this.processExecutionService = processExecutionService;
        this.policyService = policyService;
    }

    @PostMapping("/process/start")
    public ResponseEntity<ProcessInstance> startProcess(
            @RequestBody StartProcessRequest request,
            Authentication authentication
    ) {
        User user = getCurrentUser(authentication);
        return ResponseEntity.ok(
                processExecutionService.startProcess(
                        request.policyId(),
                        request.title(),
                        request.description(),
                        user
                )
        );
    }

    @PostMapping("/tasks/{taskInstanceId}/complete")
    public ResponseEntity<TaskInstance> completeTask(
            @PathVariable String taskInstanceId,
            @RequestBody CompleteTaskRequest request,
            Authentication authentication
    ) {
        User user = getCurrentUser(authentication);
        return ResponseEntity.ok(processExecutionService.completeTask(taskInstanceId, request.formData(), user));
    }

    @PostMapping("/tasks/{taskInstanceId}/take")
    public ResponseEntity<TaskInstance> takeTask(
            @PathVariable String taskInstanceId,
            Authentication authentication
    ) {
        User user = getCurrentUser(authentication);
        return ResponseEntity.ok(processExecutionService.startTask(taskInstanceId, user));
    }

    @PostMapping("/tasks/{taskInstanceId}/start")
    public ResponseEntity<TaskInstance> startTask(
            @PathVariable String taskInstanceId,
            Authentication authentication
    ) {
        User user = getCurrentUser(authentication);
        return ResponseEntity.ok(processExecutionService.startTask(taskInstanceId, user));
    }

    @GetMapping("/tasks/{taskInstanceId}")
    public ResponseEntity<TaskDetailDto> getTaskDetail(@PathVariable String taskInstanceId, Authentication authentication) {
        User user = getCurrentUser(authentication);
        return ResponseEntity.ok(processExecutionService.getTaskDetail(taskInstanceId, user));
    }

    @GetMapping("/tasks/pending/{laneId}")
    public ResponseEntity<List<PendingTaskDto>> getPendingTasksForLane(@PathVariable String laneId) {
        return ResponseEntity.ok(processExecutionService.getPendingTasksForLane(laneId));
    }

    @GetMapping("/startable-policies")
    public ResponseEntity<List<StartablePolicyDto>> getStartablePolicies(Authentication authentication) {
        User user = getCurrentUser(authentication);
        List<StartablePolicyDto> startablePolicies = policyService.getStartablePoliciesForUser(user).stream()
                .map(this::toStartablePolicyDto)
                .toList();
        return ResponseEntity.ok(startablePolicies);
    }

    @GetMapping("/my-tasks")
    public ResponseEntity<List<PendingTaskDto>> getMyTasks(Authentication authentication) {
        User user = getCurrentUser(authentication);
        return ResponseEntity.ok(processExecutionService.getMyTasks(user));
    }

    @GetMapping("/my-processes/tasks")
    public ResponseEntity<List<ProcessTaskGroupDto>> getMyProcessTaskGroups(Authentication authentication) {
        User user = getCurrentUser(authentication);
        return ResponseEntity.ok(processExecutionService.getMyProcessTaskGroups(user));
    }

    @GetMapping("/client/tasks/pending")
    @PreAuthorize("hasAuthority('CLIENT')")
    public ResponseEntity<List<PendingTaskDto>> getClientPendingTasks(Authentication authentication) {
        User user = getCurrentUser(authentication);
        return ResponseEntity.ok(processExecutionService.getMyPendingTasks(user));
    }

    private User getCurrentUser(Authentication authentication) {
        if (authentication == null || !(authentication.getPrincipal() instanceof User user)) {
            throw new RuntimeException("Usuario no autenticado");
        }
        return user;
    }

    private StartablePolicyDto toStartablePolicyDto(Policy policy) {
        return new StartablePolicyDto(policy.getId(), policy.getName(), policy.getDescription());
    }

    private record StartProcessRequest(String policyId, String title, String description) {
    }

    private record CompleteTaskRequest(String formData) {
    }
}
