package com.politicanegocio.core.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.politicanegocio.core.dto.PendingTaskDto;
import com.politicanegocio.core.dto.TaskDetailDto;
import com.politicanegocio.core.model.Policy;
import com.politicanegocio.core.model.ProcessInstance;
import com.politicanegocio.core.model.ProcessInstanceStatus;
import com.politicanegocio.core.model.TaskInstance;
import com.politicanegocio.core.model.TaskInstanceStatus;
import com.politicanegocio.core.model.User;
import com.politicanegocio.core.repository.PolicyRepository;
import com.politicanegocio.core.repository.ProcessInstanceRepository;
import com.politicanegocio.core.repository.TaskInstanceRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class ProcessExecutionService {
    private static final String CLIENT_AREA_NAME = "Cliente";
    private static final String LEGACY_CLIENT_LANE_ID = "lane_cliente";
    private static final String ROLE_CLIENT = "CLIENT";
    private static final Logger log = LoggerFactory.getLogger(ProcessExecutionService.class);

    private final ProcessInstanceRepository processInstanceRepository;
    private final TaskInstanceRepository taskInstanceRepository;
    private final PolicyRepository policyRepository;
    private final WorkflowEngine workflowEngine;
    private final PolicyService policyService;
    private final ObjectMapper objectMapper;

    public ProcessExecutionService(
            ProcessInstanceRepository processInstanceRepository,
            TaskInstanceRepository taskInstanceRepository,
            PolicyRepository policyRepository,
            WorkflowEngine workflowEngine,
            PolicyService policyService,
            ObjectMapper objectMapper
    ) {
        this.processInstanceRepository = processInstanceRepository;
        this.taskInstanceRepository = taskInstanceRepository;
        this.policyRepository = policyRepository;
        this.workflowEngine = workflowEngine;
        this.policyService = policyService;
        this.objectMapper = objectMapper;
    }

    public ProcessInstance startProcess(String policyId, User user) {
        Policy policy = policyRepository.findById(policyId)
                .orElseThrow(() -> new RuntimeException("Politica no encontrada con ID: " + policyId));

        boolean canStart = policyService.canUserStartPolicy(user, policy);
        if (!canStart) {
            throw new RuntimeException("No tienes permisos para iniciar este proceso desde tu area");
        }

        ProcessInstance processInstance = new ProcessInstance();
        processInstance.setId(UUID.randomUUID().toString());
        processInstance.setPolicyId(policyId);
        processInstance.setStatus(ProcessInstanceStatus.ACTIVE);
        processInstance.setStartedBy(user.getUsername());
        processInstance.setStartedAt(LocalDateTime.now());
        ProcessInstance savedProcessInstance = processInstanceRepository.save(processInstance);

        List<WorkflowEngine.WorkflowNode> nextNodes = workflowEngine.getNextNodes(policyId, "START", Map.of());
        
        // 🚩 LLAMADA AL NUEVO MOTOR RECURSIVO
        advanceWorkflow(savedProcessInstance, nextNodes, Map.of());
        
        return savedProcessInstance;
    }

    public TaskInstance completeTask(String taskInstanceId, String formData, User user) {
        TaskInstance taskInstance = taskInstanceRepository.findById(taskInstanceId)
                .orElseThrow(() -> new RuntimeException("Tarea no encontrada con ID: " + taskInstanceId));
        assertUserCanAccessTask(user, taskInstance);
        String username = user.getUsername();

        if (taskInstance.getStatus() == TaskInstanceStatus.PENDING) {
            throw new RuntimeException("Debes tomar la tarea antes de completarla");
        }
        if (taskInstance.getStatus() == TaskInstanceStatus.COMPLETED) {
            throw new RuntimeException("La tarea ya fue completada");
        }
        if (taskInstance.getStatus() != TaskInstanceStatus.IN_PROGRESS) {
            throw new RuntimeException("Solo se pueden completar tareas en estado IN_PROGRESS");
        }
        if (taskInstance.getAssignedTo() != null && !taskInstance.getAssignedTo().equals(username)) {
            throw new RuntimeException("La tarea esta siendo ejecutada por otro usuario");
        }

        // ==========================================================
        // 🚩 SECCIÓN: VALIDACIÓN DINÁMICA DEL FORMULARIO
        // ==========================================================
        try {
            ProcessInstance processInstance = processInstanceRepository.findById(taskInstance.getProcessInstanceId())
                    .orElseThrow(() -> new RuntimeException("Instancia de proceso no encontrada"));

            String schemaRaw = workflowEngine.getFormSchemaForNode(processInstance.getPolicyId(), taskInstance.getTaskId());
            
            if (schemaRaw != null && !schemaRaw.trim().isEmpty() && !schemaRaw.equals("[]")) {
                
                JsonNode schemaNode = objectMapper.readTree(schemaRaw);
                if (schemaNode.isTextual()) {
                    schemaNode = objectMapper.readTree(schemaNode.asText());
                }

                JsonNode dataNode = objectMapper.readTree(formData != null ? formData : "{}");
                if (dataNode.isTextual()) {
                    dataNode = objectMapper.readTree(dataNode.asText());
                }

                for (JsonNode field : schemaNode) {
                    boolean isRequired = field.has("required") && field.get("required").asBoolean();
                    
                    String fieldKey = null;
                    if (field.has("name") && !field.get("name").asText().isEmpty()) {
                        fieldKey = field.get("name").asText();
                    } else if (field.has("id")) {
                        fieldKey = field.get("id").asText();
                    }

                    if (isRequired && fieldKey != null) {
                        if (dataNode.isEmpty() || !dataNode.has(fieldKey) || dataNode.get(fieldKey).asText().trim().isEmpty()) {
                            String fieldLabel = field.has("label") ? field.get("label").asText() : fieldKey;
                            throw new RuntimeException("Validación fallida: El campo '" + fieldLabel + "' es obligatorio.");
                        }
                    }
                }
            }
        } catch (Exception e) {
            if (e instanceof RuntimeException) throw (RuntimeException) e;
            throw new RuntimeException("Error interno al procesar el formulario: " + e.getMessage());
        }
        // ==========================================================
        // 🚩 FIN DE LA VALIDACIÓN
        // ==========================================================

        taskInstance.setStatus(TaskInstanceStatus.COMPLETED);
        taskInstance.setAssignedTo(username);
        taskInstance.setFormData(formData);
        taskInstance.setCompletedAt(LocalDateTime.now());
        TaskInstance completedTask = taskInstanceRepository.save(taskInstance);

        ProcessInstance processInstance = processInstanceRepository.findById(taskInstance.getProcessInstanceId())
                .orElseThrow(() -> new RuntimeException("Instancia de proceso no encontrada"));
        Map<String, Object> routingVariables = parseRoutingVariables(formData);

        List<WorkflowEngine.WorkflowNode> nextNodes = workflowEngine.getNextNodes(
                processInstance.getPolicyId(),
                completedTask.getTaskId(),
                routingVariables
        );

        // 🚩 LLAMADA AL NUEVO MOTOR RECURSIVO PARA AVANZAR EL FLUJO
        advanceWorkflow(processInstance, nextNodes, routingVariables);
        
        return completedTask;
    }

    public TaskInstance takeTask(String taskInstanceId, User user) {
        TaskInstance taskInstance = taskInstanceRepository.findById(taskInstanceId)
                .orElseThrow(() -> new RuntimeException("Tarea no encontrada con ID: " + taskInstanceId));
        assertUserCanAccessTask(user, taskInstance);
        String username = user.getUsername();

        if (taskInstance.getStatus() == TaskInstanceStatus.COMPLETED) {
            throw new RuntimeException("La tarea ya fue completada");
        }
        if (taskInstance.getStatus() == TaskInstanceStatus.IN_PROGRESS) {
            if (username.equals(taskInstance.getAssignedTo())) {
                return taskInstance;
            }
            throw new RuntimeException("La tarea ya fue tomada por otro usuario");
        }
        if (taskInstance.getStatus() != TaskInstanceStatus.PENDING) {
            throw new RuntimeException("La tarea no puede ser tomada en su estado actual");
        }

        taskInstance.setStatus(TaskInstanceStatus.IN_PROGRESS);
        taskInstance.setAssignedTo(username);
        return taskInstanceRepository.save(taskInstance);
    }

    public List<PendingTaskDto> getPendingTasksForLane(String laneId) {
        List<TaskInstance> tasks = taskInstanceRepository.findByLaneIdAndStatus(laneId, TaskInstanceStatus.PENDING);
        if (tasks.isEmpty()) {
            return List.of();
        }

        Map<String, ProcessInstance> processById = processInstanceRepository.findAllById(
                tasks.stream().map(TaskInstance::getProcessInstanceId).collect(Collectors.toSet())
        ).stream().collect(Collectors.toMap(ProcessInstance::getId, process -> process));

        Map<String, Policy> policyById = policyRepository.findAllById(
                processById.values().stream().map(ProcessInstance::getPolicyId).collect(Collectors.toSet())
        ).stream().collect(Collectors.toMap(Policy::getId, policy -> policy));

        return tasks.stream()
                .map(task -> {
                    ProcessInstance process = processById.get(task.getProcessInstanceId());
                    String policyId = process != null ? process.getPolicyId() : "";
                    Policy policy = policyById.get(policyId);
                    String taskName = policyId.isBlank()
                            ? task.getTaskId()
                            : workflowEngine.getNodeName(policyId, task.getTaskId());
                    return new PendingTaskDto(
                            task.getId(),
                            task.getProcessInstanceId(),
                            policyId,
                            policy != null ? policy.getName() : "Proceso",
                            task.getTaskId(),
                            taskName,
                            task.getLaneId(),
                            task.getStatus(),
                            task.getCreatedAt()
                    );
                })
                .sorted(Comparator.comparing(PendingTaskDto::getCreatedAt, Comparator.nullsLast(Comparator.reverseOrder())))
                .toList();
    }

    public List<PendingTaskDto> getMyPendingTasks(User user) {
        if (isClientUser(user)) {
            return getClientPendingTasks(user);
        }
        String laneId = resolveUserLaneId(user);
        if (laneId.isBlank()) {
            return List.of();
        }
        return getPendingTasksForLane(laneId);
    }

    public List<PendingTaskDto> getMyTasks(User user) {
        if (isClientUser(user)) {
            return getMyPendingTasks(user);
        }
        String laneId = resolveUserLaneId(user);
        if (laneId.isBlank()) {
            return List.of();
        }

        Map<String, TaskInstance> deduplicated = new LinkedHashMap<>();
        for (TaskInstance task : taskInstanceRepository.findByLaneId(laneId)) {
            deduplicated.put(task.getId(), task);
        }
        for (TaskInstance task : taskInstanceRepository.findByAssignedTo(user.getUsername())) {
            deduplicated.put(task.getId(), task);
        }

        List<TaskInstance> tasks = new ArrayList<>(deduplicated.values());
        if (tasks.isEmpty()) {
            return List.of();
        }
        return mapToPendingTaskDtos(tasks);
    }

    public List<PendingTaskDto> getClientPendingTasks(User user) {
        if (user == null || user.getUsername() == null || user.getUsername().isBlank()) {
            log.warn("getClientPendingTasks: usuario nulo o sin username");
            return List.of();
        }

        String laneId = resolveUserLaneId(user);
        if (laneId.isBlank()) {
            log.warn("getClientPendingTasks: laneId vacio para usuario={}", user.getUsername());
            return List.of();
        }

        List<String> processInstanceIds = processInstanceRepository.findByStartedBy(user.getUsername().trim()).stream()
                .map(ProcessInstance::getId)
                .filter(id -> id != null && !id.isBlank())
                .toList();

        if (processInstanceIds == null || processInstanceIds.isEmpty()) {
            log.info("getClientPendingTasks: sin procesos iniciados para usuario={}", user.getUsername());
            return List.of();
        }

        List<TaskInstance> tasks = taskInstanceRepository.findByProcessInstanceIdInAndStatusAndLaneId(
                processInstanceIds,
                TaskInstanceStatus.PENDING,
                laneId
        );

        log.info(
                "getClientPendingTasks: usuario={} laneId={} processCount={} pendingMatchedCount={}",
                user.getUsername(),
                laneId,
                processInstanceIds.size(),
                tasks.size()
        );

        if (tasks.isEmpty()) {
            List<TaskInstance> allPendingForUserProcesses = taskInstanceRepository.findByProcessInstanceIdInAndStatus(
                    processInstanceIds,
                    TaskInstanceStatus.PENDING
            );
            String lanesFound = allPendingForUserProcesses.stream()
                    .map(task -> task.getLaneId() == null ? "<null>" : task.getLaneId())
                    .distinct()
                    .sorted()
                    .collect(Collectors.joining(", "));
            log.warn(
                    "getClientPendingTasks: 0 tareas para laneId={} usuario={}. Lanes pendientes encontrados en esos procesos: [{}]",
                    laneId,
                    user.getUsername(),
                    lanesFound
            );
        }

        if (tasks.isEmpty()) {
            return List.of();
        }
        return mapToPendingTaskDtos(tasks);
    }

    public TaskDetailDto getTaskDetail(String taskInstanceId, User user) {
        TaskInstance taskInstance = taskInstanceRepository.findById(taskInstanceId)
                .orElseThrow(() -> new RuntimeException("Tarea no encontrada con ID: " + taskInstanceId));
        assertUserCanAccessTask(user, taskInstance);

        ProcessInstance processInstance = processInstanceRepository.findById(taskInstance.getProcessInstanceId())
                .orElseThrow(() -> new RuntimeException("Instancia de proceso no encontrada"));
        Policy policy = policyRepository.findById(processInstance.getPolicyId())
                .orElseThrow(() -> new RuntimeException("Politica no encontrada"));

        NodeTaskDefinition nodeDefinition = extractTaskDefinition(policy.getDiagramJson(), taskInstance.getTaskId());
        String processName = policy.getName() != null ? policy.getName() : "Proceso";
        String taskName = nodeDefinition.taskName() != null && !nodeDefinition.taskName().isBlank()
                ? nodeDefinition.taskName()
                : workflowEngine.getNodeName(policy.getId(), taskInstance.getTaskId());

        return new TaskDetailDto(
                taskInstance.getId(),
                processInstance.getPolicyId(),
                taskInstance.getTaskId(),
                taskName,
                processName,
                taskInstance.getStatus(),
                nodeDefinition.description(),
                nodeDefinition.formSchema(),
                taskInstance.getFormData(),
                policy.getDiagramJson()
        );
    }

    private List<PendingTaskDto> mapToPendingTaskDtos(List<TaskInstance> tasks) {
        Set<String> processIds = tasks.stream()
                .map(TaskInstance::getProcessInstanceId)
                .collect(Collectors.toSet());

        Map<String, ProcessInstance> processById = processInstanceRepository.findAllById(processIds).stream()
                .collect(Collectors.toMap(ProcessInstance::getId, process -> process));

        Set<String> policyIds = processById.values().stream()
                .map(ProcessInstance::getPolicyId)
                .collect(Collectors.toSet());

        Map<String, Policy> policyById = policyRepository.findAllById(policyIds).stream()
                .collect(Collectors.toMap(Policy::getId, policy -> policy));

        return tasks.stream()
                .map(task -> {
                    ProcessInstance process = processById.get(task.getProcessInstanceId());
                    String policyId = process != null ? process.getPolicyId() : "";
                    Policy policy = policyById.get(policyId);
                    String taskName = policyId.isBlank()
                            ? task.getTaskId()
                            : workflowEngine.getNodeName(policyId, task.getTaskId());
                    return new PendingTaskDto(
                            task.getId(),
                            task.getProcessInstanceId(),
                            policyId,
                            policy != null ? policy.getName() : "Proceso",
                            task.getTaskId(),
                            taskName,
                            task.getLaneId(),
                            task.getStatus(),
                            task.getCreatedAt()
                    );
                })
                .sorted(Comparator.comparing(PendingTaskDto::getCreatedAt, Comparator.nullsLast(Comparator.reverseOrder())))
                .toList();
    }

    private NodeTaskDefinition extractTaskDefinition(String diagramJson, String taskId) {
        if (diagramJson == null || diagramJson.isBlank()) {
            return new NodeTaskDefinition("", "", "[]");
        }
        try {
            JsonNode root = objectMapper.readTree(diagramJson);
            JsonNode cells = root.path("cells");
            if (!cells.isArray()) {
                return new NodeTaskDefinition("", "", "[]");
            }
            for (JsonNode cell : cells) {
                if (!taskId.equals(cell.path("id").asText())) {
                    continue;
                }
                String taskName = cell.path("attrs").path("label").path("text").asText("");
                JsonNode taskForm = cell.path("nodeMeta").path("taskForm");
                String description = taskForm.path("description").asText("");
                JsonNode fields = taskForm.path("fields");
                String formSchema = fields.isArray() ? objectMapper.writeValueAsString(fields) : "[]";
                return new NodeTaskDefinition(taskName, description, formSchema);
            }
            return new NodeTaskDefinition("", "", "[]");
        } catch (Exception exception) {
            return new NodeTaskDefinition("", "", "[]");
        }
    }

    private record NodeTaskDefinition(String taskName, String description, String formSchema) {
    }

    private String resolveUserLaneId(User user) {
        if (user == null) {
            return "";
        }
        if (user.getLaneId() != null && !user.getLaneId().isBlank()) {
            return normalizeLaneId(user.getLaneId());
        }
        return user.getArea() != null ? normalizeLaneId(user.getArea()) : "";
    }

    // =========================================================================
    // 🚩 EL NUEVO MOTOR DE ORQUESTACIÓN RECURSIVO
    // =========================================================================
    private void advanceWorkflow(
            ProcessInstance processInstance,
            List<WorkflowEngine.WorkflowNode> nextNodes,
            Map<String, Object> routingVariables
    ) {
        if (nextNodes == null || nextNodes.isEmpty()) return;

        for (WorkflowEngine.WorkflowNode node : nextNodes) {
            String type = node.nodeType().toUpperCase();

            if ("FORK".equals(type)) {
                // Salta automáticamente a los siguientes caminos
                List<WorkflowEngine.WorkflowNode> nextFromFork = workflowEngine.getNextNodes(
                        processInstance.getPolicyId(),
                        node.nodeId(),
                        routingVariables
                );
                advanceWorkflow(processInstance, nextFromFork, routingVariables); 
            }
            else if ("DECISION".equals(type)) {
                List<WorkflowEngine.WorkflowNode> nextFromDecision = workflowEngine.getNextNodes(
                        processInstance.getPolicyId(),
                        node.nodeId(),
                        routingVariables
                );
                advanceWorkflow(processInstance, nextFromDecision, routingVariables);
            } 
            else if ("JOIN".equals(type)) {
                // Espera a que todas las ramas entrantes terminen
                List<String> incomingIds = workflowEngine.getIncomingNodeIds(processInstance.getPolicyId(), node.nodeId());
                
                boolean allCompleted = true;
                for (String incomingId : incomingIds) {
                    boolean isCompleted = taskInstanceRepository.existsByProcessInstanceIdAndTaskIdAndStatus(
                            processInstance.getId(), incomingId, TaskInstanceStatus.COMPLETED);
                    if (!isCompleted) {
                        allCompleted = false;
                        break;
                    }
                }

                if (allCompleted) {
                    List<WorkflowEngine.WorkflowNode> nextFromJoin = workflowEngine.getNextNodes(
                            processInstance.getPolicyId(),
                            node.nodeId(),
                            routingVariables
                    );
                    advanceWorkflow(processInstance, nextFromJoin, routingVariables);
                }
            } 
            else if ("END".equals(type)) {
                // Marca el proceso como terminado
                processInstance.setStatus(ProcessInstanceStatus.COMPLETED);
                processInstance.setCompletedAt(LocalDateTime.now());
                processInstanceRepository.save(processInstance);
            } 
            else {
                // Tarea normal o cualquier otra cosa, la creamos para que la tome un humano
                createSinglePendingTask(processInstance.getId(), node);
            }
        }
    }

    private Map<String, Object> parseRoutingVariables(String rawFormData) {
        if (rawFormData == null || rawFormData.isBlank()) {
            return Map.of();
        }
        try {
            JsonNode node = objectMapper.readTree(rawFormData);
            if (node.isTextual()) {
                node = objectMapper.readTree(node.asText());
            }
            if (!node.isObject()) {
                return Map.of();
            }
            return objectMapper.convertValue(
                    node,
                    objectMapper.getTypeFactory().constructMapType(Map.class, String.class, Object.class)
            );
        } catch (Exception exception) {
            return Map.of();
        }
    }

    private void createSinglePendingTask(String processInstanceId, WorkflowEngine.WorkflowNode node) {
        // Prevenir duplicados si un JOIN o un proceso se dispara varias veces
        boolean alreadyPending = taskInstanceRepository.existsByProcessInstanceIdAndTaskIdAndStatusNot(
                processInstanceId, node.nodeId(), TaskInstanceStatus.COMPLETED);
        
        if (!alreadyPending) {
            TaskInstance taskInstance = new TaskInstance();
            taskInstance.setId(UUID.randomUUID().toString());
            taskInstance.setProcessInstanceId(processInstanceId);
            taskInstance.setTaskId(node.nodeId());
            taskInstance.setLaneId(normalizeLaneId(node.laneId()));
            taskInstance.setStatus(TaskInstanceStatus.PENDING);
            taskInstance.setCreatedAt(LocalDateTime.now());
            taskInstanceRepository.save(taskInstance);
            log.info(
                    "createSinglePendingTask: processInstanceId={} taskId={} laneRaw={} laneSaved={}",
                    processInstanceId,
                    node.nodeId(),
                    node.laneId(),
                    taskInstance.getLaneId()
            );
        }
    }

    private String normalizeLaneId(String laneId) {
        if (laneId == null) {
            return "";
        }
        String normalized = laneId.trim();
        if (LEGACY_CLIENT_LANE_ID.equalsIgnoreCase(normalized) || CLIENT_AREA_NAME.equalsIgnoreCase(normalized)) {
            return CLIENT_AREA_NAME;
        }
        return normalized;
    }

    private boolean isClientUser(User user) {
        return user != null && user.getRoles() != null && user.getRoles().contains(ROLE_CLIENT);
    }

    private void assertUserCanAccessTask(User user, TaskInstance taskInstance) {
        if (user == null || taskInstance == null) {
            throw new RuntimeException("Acceso denegado a la tarea");
        }
        ProcessInstance processInstance = processInstanceRepository.findById(taskInstance.getProcessInstanceId())
                .orElseThrow(() -> new RuntimeException("Instancia de proceso no encontrada"));

        if (isClientUser(user)) {
            String username = user.getUsername() == null ? "" : user.getUsername().trim();
            String startedBy = processInstance.getStartedBy() == null ? "" : processInstance.getStartedBy().trim();
            if (!username.equals(startedBy)) {
                throw new RuntimeException("No tienes permisos para acceder a esta tarea");
            }
            String userLane = resolveUserLaneId(user);
            String taskLane = normalizeLaneId(taskInstance.getLaneId());
            if (!userLane.equals(taskLane)) {
                throw new RuntimeException("La tarea no pertenece a tu carril");
            }
            return;
        }

        String userLane = resolveUserLaneId(user);
        String taskLane = normalizeLaneId(taskInstance.getLaneId());
        boolean isAssignedToUser = taskInstance.getAssignedTo() != null
                && taskInstance.getAssignedTo().equals(user.getUsername());
        if (!isAssignedToUser && !userLane.equals(taskLane)) {
            throw new RuntimeException("No tienes permisos para acceder a esta tarea");
        }
    }
}
