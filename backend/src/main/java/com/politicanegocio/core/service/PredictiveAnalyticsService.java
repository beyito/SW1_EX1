package com.politicanegocio.core.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.politicanegocio.core.dto.PredictiveAnalysisResponseDto;
import com.politicanegocio.core.model.DocumentRecord;
import com.politicanegocio.core.model.Policy;
import com.politicanegocio.core.model.ProcessInstance;
import com.politicanegocio.core.model.TaskInstance;
import com.politicanegocio.core.model.User;
import com.politicanegocio.core.repository.DocumentRecordRepository;
import com.politicanegocio.core.repository.PolicyRepository;
import com.politicanegocio.core.repository.ProcessInstanceRepository;
import com.politicanegocio.core.repository.TaskInstanceRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class PredictiveAnalyticsService {
    private static final Logger log = LoggerFactory.getLogger(PredictiveAnalyticsService.class);

    private final RestClient restClient;
    private final ObjectMapper objectMapper;
    private final PolicyRepository policyRepository;
    private final ProcessInstanceRepository processInstanceRepository;
    private final TaskInstanceRepository taskInstanceRepository;
    private final DocumentRecordRepository documentRecordRepository;
    private final WorkflowEngine workflowEngine;

    public PredictiveAnalyticsService(
            @Value("${app.ai-engine.base-url:http://127.0.0.1:8010}") String aiEngineBaseUrl,
            ObjectMapper objectMapper,
            PolicyRepository policyRepository,
            ProcessInstanceRepository processInstanceRepository,
            TaskInstanceRepository taskInstanceRepository,
            DocumentRecordRepository documentRecordRepository,
            WorkflowEngine workflowEngine
    ) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(15000);
        factory.setReadTimeout(120000);
        this.restClient = RestClient.builder()
                .baseUrl(aiEngineBaseUrl)
                .requestFactory(factory)
                .build();
        this.objectMapper = objectMapper;
        this.policyRepository = policyRepository;
        this.processInstanceRepository = processInstanceRepository;
        this.taskInstanceRepository = taskInstanceRepository;
        this.documentRecordRepository = documentRecordRepository;
        this.workflowEngine = workflowEngine;
    }

    public PredictiveAnalysisResponseDto analyze(User user) {
        if (user == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Usuario no autenticado.");
        }

        List<Policy> policies = resolvePolicies(user);
        Set<String> policyIds = policies.stream()
                .map(Policy::getId)
                .filter(id -> id != null && !id.isBlank())
                .collect(Collectors.toSet());

        if (policyIds.isEmpty()) {
            return emptyResponse("No hay políticas disponibles para analizar en tu empresa.");
        }

        List<ProcessInstance> processes = processInstanceRepository.findAll().stream()
                .filter(process -> process.getPolicyId() != null && policyIds.contains(process.getPolicyId()))
                .toList();
        List<String> processIds = processes.stream()
                .map(ProcessInstance::getId)
                .filter(id -> id != null && !id.isBlank())
                .toList();

        List<TaskInstance> tasks = processIds.isEmpty()
                ? List.of()
                : taskInstanceRepository.findByProcessInstanceIdIn(processIds);
        List<DocumentRecord> documents = policyIds.isEmpty()
                ? List.of()
                : documentRecordRepository.findByPolicyIdInOrderByCreatedAtDesc(policyIds.stream().toList());

        Map<String, String> policyByProcess = processes.stream()
                .filter(process -> process.getId() != null && process.getPolicyId() != null)
                .collect(Collectors.toMap(ProcessInstance::getId, ProcessInstance::getPolicyId, (left, right) -> left));

        Map<String, Object> payload = new HashMap<>();
        payload.put("processes", processes.stream().map(this::toProcessEvent).toList());
        payload.put("tasks", tasks.stream().map(task -> toTaskEvent(task, policyByProcess.getOrDefault(task.getProcessInstanceId(), ""))).toList());
        payload.put("documents", documents.stream().map(this::toDocumentEvent).toList());

        String requestId = UUID.randomUUID().toString();
        try {
            String rawResponse = restClient.post()
                    .uri("/api/v1/prediction/analyze")
                    .header("X-Request-Id", requestId)
                    .body(payload)
                    .retrieve()
                    .body(String.class);
            PredictiveAnalysisResponseDto response = parseResponse(rawResponse);
            return enrichResponse(response, policies, processes, documents);
        } catch (ResourceAccessException exception) {
            log.error("Predictive analytics AI connectivity error requestId={}", requestId, exception);
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "El motor predictivo no está disponible.", exception);
        } catch (RestClientResponseException exception) {
            log.error("Predictive analytics AI bad response requestId={} status={} body={}", requestId, exception.getStatusCode(), exception.getResponseBodyAsString(), exception);
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "El motor predictivo devolvió una respuesta inválida.", exception);
        } catch (Exception exception) {
            log.error("Predictive analytics unexpected error requestId={}", requestId, exception);
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "No se pudo ejecutar el análisis predictivo.", exception);
        }
    }

    private List<Policy> resolvePolicies(User user) {
        if (user.getRoles() == null || !user.getRoles().contains("COMPANY_ADMIN")) {
            throw new AccessDeniedException("Solo los administradores de empresa pueden ejecutar predicciones.");
        }
        if (user.getCompany() == null || user.getCompany().isBlank()) {
            return List.of();
        }
        return policyRepository.findByCompanyId(user.getCompany());
    }

    private Map<String, Object> toProcessEvent(ProcessInstance process) {
        Map<String, Object> item = new HashMap<>();
        item.put("processInstanceId", safe(process.getId()));
        item.put("policyId", safe(process.getPolicyId()));
        item.put("title", safe(process.getTitle()));
        item.put("description", safe(process.getDescription()));
        item.put("status", process.getStatus() == null ? "" : process.getStatus().name());
        item.put("startedBy", safe(process.getStartedBy()));
        item.put("startedAt", format(process.getStartedAt()));
        item.put("completedAt", format(process.getCompletedAt()));
        return item;
    }

    private Map<String, Object> toTaskEvent(TaskInstance task, String policyId) {
        Map<String, Object> item = new HashMap<>();
        item.put("processInstanceId", safe(task.getProcessInstanceId()));
        item.put("policyId", safe(policyId));
        item.put("taskId", safe(task.getTaskId()));
        item.put("laneId", safe(task.getLaneId()));
        item.put("status", task.getStatus() == null ? "" : task.getStatus().name());
        item.put("createdAt", format(task.getCreatedAt()));
        item.put("startedAt", format(task.getStartedAt()));
        item.put("completedAt", format(task.getCompletedAt()));
        return item;
    }

    private Map<String, Object> toDocumentEvent(DocumentRecord document) {
        Map<String, Object> item = new HashMap<>();
        item.put("processInstanceId", safe(document.getProcessInstanceId()));
        item.put("policyId", safe(document.getPolicyId()));
        item.put("documentId", safe(document.getId()));
        item.put("createdBy", safe(document.getCreatedBy()));
        item.put("createdAt", format(document.getCreatedAt()));
        item.put("size", document.getSize());
        return item;
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }

    private String format(LocalDateTime value) {
        return value == null ? null : value.toString();
    }

    private PredictiveAnalysisResponseDto parseResponse(String rawResponse) throws Exception {
        JsonNode root = objectMapper.readTree(rawResponse == null ? "{}" : rawResponse);
        return new PredictiveAnalysisResponseDto(
                root.path("model_strategy").asText("unknown"),
                readList(root, "anomalies"),
                readList(root, "priorities"),
                readList(root, "route_predictions"),
                readList(root, "bottlenecks"),
                readStringList(root, "warnings")
        );
    }

    private List<Map<String, Object>> readList(JsonNode root, String field) {
        JsonNode node = root.path(field);
        if (!node.isArray()) {
            return List.of();
        }
        return objectMapper.convertValue(
                node,
                objectMapper.getTypeFactory().constructCollectionType(List.class, Map.class)
        );
    }

    private List<String> readStringList(JsonNode root, String field) {
        JsonNode node = root.path(field);
        if (!node.isArray()) {
            return List.of();
        }
        return objectMapper.convertValue(
                node,
                objectMapper.getTypeFactory().constructCollectionType(List.class, String.class)
        );
    }

    private PredictiveAnalysisResponseDto enrichResponse(
            PredictiveAnalysisResponseDto response,
            List<Policy> policies,
            List<ProcessInstance> processes,
            List<DocumentRecord> documents
    ) {
        Map<String, String> policyNames = policies.stream()
                .filter(policy -> policy.getId() != null)
                .collect(Collectors.toMap(
                        Policy::getId,
                        policy -> readable(policy.getName(), "Trámite sin nombre"),
                        (left, right) -> left
                ));

        Map<String, String> processTitles = processes.stream()
                .filter(process -> process.getId() != null)
                .collect(Collectors.toMap(
                        ProcessInstance::getId,
                        process -> readable(process.getTitle(), policyNames.getOrDefault(process.getPolicyId(), "Instancia de trámite")),
                        (left, right) -> left
                ));

        Map<String, String> documentNames = documents.stream()
                .filter(document -> document.getId() != null)
                .collect(Collectors.toMap(
                        DocumentRecord::getId,
                        document -> readable(document.getFileName(), readable(document.getDocumentCode(), "Documento del trámite")),
                        (left, right) -> left
                ));

        Map<String, String> laneNames = policies.stream()
                .flatMap(policy -> policy.getLanes() == null ? List.<com.politicanegocio.core.model.Lane>of().stream() : policy.getLanes().stream())
                .filter(lane -> lane.getId() != null)
                .collect(Collectors.toMap(
                        com.politicanegocio.core.model.Lane::getId,
                        lane -> readable(lane.getName(), humanizeIdentifier(lane.getId(), "Carril del flujo")),
                        (left, right) -> left
                ));

        response.priorities().forEach(item -> enrichCommon(item, policyNames, processTitles, documentNames, laneNames));
        response.anomalies().forEach(item -> enrichCommon(item, policyNames, processTitles, documentNames, laneNames));
        response.bottlenecks().forEach(item -> enrichCommon(item, policyNames, processTitles, documentNames, laneNames));
        response.routePredictions().forEach(item -> {
            String policyId = asString(item.get("policyId"));
            String currentTaskId = firstNonBlank(asString(item.get("currentTaskId")), asString(item.get("taskId")));
            String nextTaskId = asString(item.get("predictedNextTaskId"));
            item.put("currentTaskId", currentTaskId);
            item.put("currentTaskLabel", resolveTaskLabel(policyId, currentTaskId, "Paso actual"));
            item.put("predictedNextTaskLabel", resolveTaskLabel(policyId, nextTaskId, "Siguiente paso"));
            if (!policyId.isBlank()) {
                item.put("policyName", policyNames.getOrDefault(policyId, "Trámite sin nombre"));
            }
        });
        return response;
    }

    private void enrichCommon(
            Map<String, Object> item,
            Map<String, String> policyNames,
            Map<String, String> processTitles,
            Map<String, String> documentNames,
            Map<String, String> laneNames
    ) {
        String policyId = asString(item.get("policyId"));
        String processInstanceId = asString(item.get("processInstanceId"));
        String documentId = asString(item.get("documentId"));
        String laneId = asString(item.get("laneId"));
        String taskId = asString(item.get("taskId"));

        if (!policyId.isBlank()) {
            item.put("policyName", policyNames.getOrDefault(policyId, "Trámite sin nombre"));
        }
        if (!processInstanceId.isBlank()) {
            item.put("processTitle", processTitles.getOrDefault(processInstanceId, "Instancia de trámite"));
        }
        if (!documentId.isBlank()) {
            item.put("documentName", documentNames.getOrDefault(documentId, "Documento del trámite"));
        }
        if (!laneId.isBlank()) {
            item.put("laneName", laneNames.getOrDefault(laneId, humanizeIdentifier(laneId, "Carril del flujo")));
        }
        if (!taskId.isBlank()) {
            item.put("taskLabel", humanizeIdentifier(taskId, "Tarea del flujo"));
        }
    }

    private String asString(Object value) {
        return value == null ? "" : value.toString();
    }

    private String readable(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }

    private String resolveTaskLabel(String policyId, String taskId, String fallback) {
        if (!policyId.isBlank() && !taskId.isBlank()) {
            String nodeName = workflowEngine.getNodeName(policyId, taskId);
            if (nodeName != null && !nodeName.isBlank()) {
                return nodeName;
            }
        }
        return humanizeIdentifier(taskId, fallback);
    }

    private String firstNonBlank(String first, String second) {
        return first == null || first.isBlank() ? second : first;
    }

    private String humanizeIdentifier(String value, String fallback) {
        if (value == null || value.isBlank()) {
            return fallback;
        }
        if (value.matches("(?i)^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$")) {
            return fallback;
        }
        return value.replace("_", " ").replace("-", " ").trim();
    }

    private PredictiveAnalysisResponseDto emptyResponse(String warning) {
        return new PredictiveAnalysisResponseDto("empty", List.of(), List.of(), List.of(), List.of(), List.of(warning));
    }
}
