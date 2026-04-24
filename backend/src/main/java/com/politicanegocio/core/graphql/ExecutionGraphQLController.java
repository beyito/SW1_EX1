package com.politicanegocio.core.graphql;

import com.politicanegocio.core.dto.PendingTaskDto;
import com.politicanegocio.core.dto.TaskDetailDto;
import com.politicanegocio.core.model.User;
import com.politicanegocio.core.service.ProcessExecutionService;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;

import java.util.List;

@Controller
public class ExecutionGraphQLController {

    private final ProcessExecutionService processExecutionService;

    public ExecutionGraphQLController(ProcessExecutionService processExecutionService) {
        this.processExecutionService = processExecutionService;
    }

    @QueryMapping
    public List<PendingTaskDto> myTasks(Authentication authentication) {
        User user = getCurrentUser(authentication);
        return processExecutionService.getMyTasks(user);
    }

    @QueryMapping
    public TaskDetailDto getTaskDetail(@Argument String taskId, Authentication authentication) {
        User user = getCurrentUser(authentication);
        return processExecutionService.getTaskDetail(taskId, user);
    }

    @MutationMapping
    public TaskDetailDto takeTask(@Argument String taskId, Authentication authentication) {
        User user = getCurrentUser(authentication);
        processExecutionService.takeTask(taskId, user);
        return processExecutionService.getTaskDetail(taskId, user);
    }

    @MutationMapping
    public TaskDetailDto completeTask(@Argument String taskId, @Argument String formData, Authentication authentication) {
        User user = getCurrentUser(authentication);
        processExecutionService.completeTask(taskId, formData, user);
        return processExecutionService.getTaskDetail(taskId, user);
    }

    private User getCurrentUser(Authentication authentication) {
        if (authentication == null || !(authentication.getPrincipal() instanceof User user)) {
            throw new RuntimeException("Usuario no autenticado");
        }
        return user;
    }
}
