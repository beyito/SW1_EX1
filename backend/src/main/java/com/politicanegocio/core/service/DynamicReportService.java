package com.politicanegocio.core.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.politicanegocio.core.dto.DynamicReportPlanDto;
import com.politicanegocio.core.dto.DynamicReportRequestDto;
import com.politicanegocio.core.dto.GeneratedReportFileDto;
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
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.server.ResponseStatusException;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class DynamicReportService {
    private static final Logger log = LoggerFactory.getLogger(DynamicReportService.class);
    private static final MediaType EXCEL_MEDIA_TYPE = MediaType.parseMediaType("application/vnd.ms-excel");
    private static final MediaType CSV_MEDIA_TYPE = MediaType.parseMediaType("text/csv");
    private static final MediaType PDF_MEDIA_TYPE = MediaType.APPLICATION_PDF;

    private final RestClient restClient;
    private final ObjectMapper objectMapper;
    private final PolicyRepository policyRepository;
    private final ProcessInstanceRepository processInstanceRepository;
    private final TaskInstanceRepository taskInstanceRepository;
    private final DocumentRecordRepository documentRecordRepository;

    public DynamicReportService(
            @Value("${app.ai-engine.base-url:http://127.0.0.1:8010}") String aiEngineBaseUrl,
            ObjectMapper objectMapper,
            PolicyRepository policyRepository,
            ProcessInstanceRepository processInstanceRepository,
            TaskInstanceRepository taskInstanceRepository,
            DocumentRecordRepository documentRecordRepository
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
    }

    public DynamicReportPlanDto plan(User user, DynamicReportRequestDto request) {
        validateUser(user);
        String prompt = request == null ? "" : safe(request.prompt());
        if (prompt.isBlank()) {
            return incomplete("Para generar el reporte necesito que escribas datos, criterios y formato.");
        }

        List<Policy> policies = policyRepository.findByCompanyId(user.getCompany());
        Set<String> policyIds = policies.stream()
                .map(Policy::getId)
                .filter(id -> id != null && !id.isBlank())
                .collect(Collectors.toSet());
        if (policyIds.isEmpty()) {
            return incomplete("No hay tramites disponibles para generar reportes en tu empresa.");
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
        List<DocumentRecord> documents = documentRecordRepository.findByPolicyIdInOrderByCreatedAtDesc(policyIds.stream().toList());

        Map<String, String> policyByProcess = processes.stream()
                .filter(process -> process.getId() != null && process.getPolicyId() != null)
                .collect(Collectors.toMap(ProcessInstance::getId, ProcessInstance::getPolicyId, (left, right) -> left));

        Map<String, Object> payload = new HashMap<>();
        payload.put("prompt", prompt);
        payload.put("processes", processes.stream().map(this::toProcessEvent).toList());
        payload.put("tasks", tasks.stream().map(task -> toTaskEvent(task, policyByProcess.getOrDefault(task.getProcessInstanceId(), ""))).toList());
        payload.put("documents", documents.stream().map(this::toDocumentEvent).toList());

        String requestId = UUID.randomUUID().toString();
        try {
            String rawResponse = restClient.post()
                    .uri("/api/v1/analytics/report-plan")
                    .header("X-Request-Id", requestId)
                    .body(payload)
                    .retrieve()
                    .body(String.class);
            DynamicReportPlanDto plan = parsePlan(rawResponse);
            return enrichPlan(plan, policies, processes, documents);
        } catch (ResourceAccessException exception) {
            log.error("Dynamic report AI connectivity error requestId={}", requestId, exception);
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "El agente analitico no esta disponible.", exception);
        } catch (RestClientResponseException exception) {
            log.error("Dynamic report AI bad response requestId={} status={} body={}", requestId, exception.getStatusCode(), exception.getResponseBodyAsString(), exception);
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "El agente analitico devolvio una respuesta invalida.", exception);
        } catch (Exception exception) {
            log.error("Dynamic report unexpected error requestId={}", requestId, exception);
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "No se pudo preparar el reporte dinamico.", exception);
        }
    }

    public GeneratedReportFileDto generate(User user, DynamicReportRequestDto request) {
        DynamicReportPlanDto reportPlan = plan(user, request);
        if (!reportPlan.complete()) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, reportPlan.question());
        }

        String format = safe(reportPlan.format()).toLowerCase();
        String baseName = slug(reportPlan.title().isBlank() ? "reporte-dinamico" : reportPlan.title());
        if ("pdf".equals(format)) {
            return new GeneratedReportFileDto(baseName + ".pdf", PDF_MEDIA_TYPE, renderPdf(reportPlan));
        }
        if ("excel".equals(format) || "xls".equals(format) || "xlsx".equals(format)) {
            return new GeneratedReportFileDto(baseName + ".xls", EXCEL_MEDIA_TYPE, renderExcel(reportPlan));
        }
        return new GeneratedReportFileDto(baseName + ".csv", CSV_MEDIA_TYPE, renderCsv(reportPlan));
    }

    private void validateUser(User user) {
        if (user == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Usuario no autenticado.");
        }
        if (user.getRoles() == null || !user.getRoles().contains("COMPANY_ADMIN")) {
            throw new AccessDeniedException("Solo los administradores de empresa pueden generar reportes.");
        }
        if (user.getCompany() == null || user.getCompany().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El usuario no tiene empresa asociada.");
        }
    }

    private DynamicReportPlanDto parsePlan(String rawResponse) throws Exception {
        JsonNode root = objectMapper.readTree(rawResponse == null ? "{}" : rawResponse);
        return new DynamicReportPlanDto(
                root.path("complete").asBoolean(false),
                readStringList(root, "missing_fields"),
                root.path("question").asText(""),
                root.path("data_scope").asText(""),
                readMap(root, "criteria"),
                root.path("format").asText(""),
                root.path("title").asText("Reporte dinamico"),
                root.path("summary").asText(""),
                readList(root, "rows"),
                readStringList(root, "warnings")
        );
    }

    private DynamicReportPlanDto enrichPlan(
            DynamicReportPlanDto plan,
            List<Policy> policies,
            List<ProcessInstance> processes,
            List<DocumentRecord> documents
    ) {
        Map<String, String> policyNames = policies.stream()
                .filter(policy -> policy.getId() != null)
                .collect(Collectors.toMap(Policy::getId, policy -> readable(policy.getName(), "Tramite sin nombre"), (left, right) -> left));
        Map<String, String> processTitles = processes.stream()
                .filter(process -> process.getId() != null)
                .collect(Collectors.toMap(
                        ProcessInstance::getId,
                        process -> readable(process.getTitle(), policyNames.getOrDefault(process.getPolicyId(), "Instancia de tramite")),
                        (left, right) -> left
                ));
        Map<String, String> documentNames = documents.stream()
                .filter(document -> document.getId() != null)
                .collect(Collectors.toMap(
                        DocumentRecord::getId,
                        document -> readable(document.getFileName(), readable(document.getDocumentCode(), "Documento del tramite")),
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

        List<Map<String, Object>> enrichedRows = plan.rows().stream()
                .map(row -> {
                    Map<String, Object> copy = new HashMap<>(row);
                    enrichRow(copy, policyNames, processTitles, documentNames, laneNames);
                    return copy;
                })
                .toList();

        return new DynamicReportPlanDto(
                plan.complete(),
                plan.missingFields(),
                plan.question(),
                plan.dataScope(),
                plan.criteria(),
                plan.format(),
                plan.title(),
                plan.summary(),
                enrichedRows,
                plan.warnings()
        );
    }

    private void enrichRow(
            Map<String, Object> row,
            Map<String, String> policyNames,
            Map<String, String> processTitles,
            Map<String, String> documentNames,
            Map<String, String> laneNames
    ) {
        String policyId = asString(row.get("policyId"));
        String processInstanceId = asString(row.get("processInstanceId"));
        String documentId = asString(row.get("documentId"));
        String laneId = asString(row.get("laneId"));
        String taskId = asString(row.get("taskId"));
        if (!policyId.isBlank()) {
            row.put("policyName", policyNames.getOrDefault(policyId, "Tramite sin nombre"));
        }
        if (!processInstanceId.isBlank()) {
            row.put("processTitle", processTitles.getOrDefault(processInstanceId, "Instancia de tramite"));
        }
        if (!documentId.isBlank()) {
            row.put("documentName", documentNames.getOrDefault(documentId, "Documento del tramite"));
        }
        if (!laneId.isBlank()) {
            row.put("laneName", laneNames.getOrDefault(laneId, humanizeIdentifier(laneId, "Carril del flujo")));
        }
        if (!taskId.isBlank()) {
            row.put("taskLabel", humanizeIdentifier(taskId, "Tarea del flujo"));
        }
    }

    private byte[] renderCsv(DynamicReportPlanDto plan) {
        List<String> columns = columns(plan.rows());
        StringBuilder builder = new StringBuilder();
        builder.append(plan.title()).append("\n");
        builder.append(plan.summary()).append("\n\n");
        builder.append(columns.stream().map(this::csv).collect(Collectors.joining(","))).append("\n");
        for (Map<String, Object> row : plan.rows()) {
            builder.append(columns.stream().map(column -> csv(asString(row.get(column)))).collect(Collectors.joining(","))).append("\n");
        }
        return builder.toString().getBytes(StandardCharsets.UTF_8);
    }

    private byte[] renderExcel(DynamicReportPlanDto plan) {
        List<String> columns = columns(plan.rows());
        StringBuilder builder = new StringBuilder();
        builder.append("<html><head><meta charset=\"UTF-8\"></head><body>");
        builder.append("<h1>").append(html(plan.title())).append("</h1>");
        builder.append("<p>").append(html(plan.summary())).append("</p>");
        builder.append("<table border=\"1\"><thead><tr>");
        for (String column : columns) {
            builder.append("<th>").append(html(label(column))).append("</th>");
        }
        builder.append("</tr></thead><tbody>");
        for (Map<String, Object> row : plan.rows()) {
            builder.append("<tr>");
            for (String column : columns) {
                builder.append("<td>").append(html(asString(row.get(column)))).append("</td>");
            }
            builder.append("</tr>");
        }
        builder.append("</tbody></table></body></html>");
        return builder.toString().getBytes(StandardCharsets.UTF_8);
    }

    private byte[] renderPdf(DynamicReportPlanDto plan) {
        List<String> lines = new ArrayList<>();
        lines.add(plan.title());
        lines.add(plan.summary());
        plan.warnings().forEach(warning -> lines.add("Aviso: " + warning));
        lines.add("");
        List<String> columns = columns(plan.rows());
        if (columns.isEmpty()) {
            lines.add("Sin registros para mostrar.");
        } else {
            lines.addAll(pdfTableLines(columns, plan.rows()));
        }
        return simplePdf(lines);
    }

    private List<String> pdfTableLines(List<String> columns, List<Map<String, Object>> rows) {
        List<String> visibleColumns = columns.stream().limit(5).toList();
        Map<String, Integer> widths = new HashMap<>();
        for (String column : visibleColumns) {
            int maxValue = rows.stream()
                    .map(row -> asString(row.get(column)))
                    .mapToInt(value -> Math.min(value.length(), 22))
                    .max()
                    .orElse(8);
            widths.put(column, Math.min(Math.max(label(column).length(), maxValue), 22));
        }

        List<String> lines = new ArrayList<>();
        lines.add(visibleColumns.stream()
                .map(column -> pad(shorten(label(column), widths.get(column)), widths.get(column)))
                .collect(Collectors.joining(" | ")));
        lines.add(visibleColumns.stream()
                .map(column -> "-".repeat(widths.get(column)))
                .collect(Collectors.joining("-+-")));
        for (Map<String, Object> row : rows) {
            lines.add(visibleColumns.stream()
                    .map(column -> pad(shorten(asString(row.get(column)), widths.get(column)), widths.get(column)))
                    .collect(Collectors.joining(" | ")));
        }
        if (columns.size() > visibleColumns.size()) {
            lines.add("");
            lines.add("Nota: el PDF muestra las primeras " + visibleColumns.size() + " columnas. Usa Excel/CSV para ver el detalle completo.");
        }
        return lines;
    }

    private String pad(String value, int width) {
        String safeValue = safe(value);
        if (safeValue.length() >= width) {
            return safeValue;
        }
        return safeValue + " ".repeat(width - safeValue.length());
    }

    private byte[] simplePdf(List<String> lines) {
        StringBuilder text = new StringBuilder("BT /F1 9 Tf 11 TL 40 800 Td ");
        int count = 0;
        for (String line : lines) {
            if (count++ >= 65) {
                break;
            }
            text.append("(").append(pdf(line)).append(") Tj T* ");
        }
        text.append("ET");
        byte[] stream = text.toString().getBytes(StandardCharsets.UTF_8);
        List<String> objects = List.of(
                "<< /Type /Catalog /Pages 2 0 R >>",
                "<< /Type /Pages /Kids [3 0 R] /Count 1 >>",
                "<< /Type /Page /Parent 2 0 R /MediaBox [0 0 595 842] /Resources << /Font << /F1 4 0 R >> >> /Contents 5 0 R >>",
                "<< /Type /Font /Subtype /Type1 /BaseFont /Courier >>",
                "<< /Length " + stream.length + " >>\nstream\n" + new String(stream, StandardCharsets.UTF_8) + "\nendstream"
        );
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        write(out, "%PDF-1.4\n");
        List<Integer> offsets = new ArrayList<>();
        for (int i = 0; i < objects.size(); i++) {
            offsets.add(out.size());
            write(out, (i + 1) + " 0 obj\n" + objects.get(i) + "\nendobj\n");
        }
        int xref = out.size();
        write(out, "xref\n0 " + (objects.size() + 1) + "\n0000000000 65535 f \n");
        for (Integer offset : offsets) {
            write(out, String.format("%010d 00000 n \n", offset));
        }
        write(out, "trailer << /Root 1 0 R /Size " + (objects.size() + 1) + " >>\nstartxref\n" + xref + "\n%%EOF");
        return out.toByteArray();
    }

    private void write(ByteArrayOutputStream out, String value) {
        out.writeBytes(value.getBytes(StandardCharsets.UTF_8));
    }

    private List<String> columns(List<Map<String, Object>> rows) {
        LinkedHashSet<String> columns = new LinkedHashSet<>();
        for (Map<String, Object> row : rows) {
            row.keySet().stream()
                    .filter(key -> !key.endsWith("Id"))
                    .forEach(columns::add);
        }
        return columns.stream().toList();
    }

    private String label(String column) {
        return switch (column) {
            case "policyName" -> "Tramite";
            case "processTitle" -> "Instancia";
            case "documentName" -> "Documento";
            case "laneName" -> "Area/Carril";
            case "taskLabel" -> "Tarea";
            case "risk" -> "Riesgo";
            case "score" -> "Score";
            case "reason" -> "Explicacion";
            default -> humanizeIdentifier(column, column);
        };
    }

    private Map<String, Object> readMap(JsonNode root, String field) {
        JsonNode node = root.path(field);
        if (!node.isObject()) {
            return Map.of();
        }
        return objectMapper.convertValue(node, objectMapper.getTypeFactory().constructMapType(Map.class, String.class, Object.class));
    }

    private List<Map<String, Object>> readList(JsonNode root, String field) {
        JsonNode node = root.path(field);
        if (!node.isArray()) {
            return List.of();
        }
        return objectMapper.convertValue(node, objectMapper.getTypeFactory().constructCollectionType(List.class, Map.class));
    }

    private List<String> readStringList(JsonNode root, String field) {
        JsonNode node = root.path(field);
        if (!node.isArray()) {
            return List.of();
        }
        return objectMapper.convertValue(node, objectMapper.getTypeFactory().constructCollectionType(List.class, String.class));
    }

    private DynamicReportPlanDto incomplete(String question) {
        return new DynamicReportPlanDto(false, List.of(), question, "", Map.of(), "", "Solicitud incompleta", question, List.of(), List.of());
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

    private String asString(Object value) {
        return value == null ? "" : value.toString();
    }

    private String readable(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
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

    private String format(LocalDateTime value) {
        return value == null ? null : value.toString();
    }

    private String slug(String value) {
        String normalized = safe(value).toLowerCase().replaceAll("[^a-z0-9]+", "-").replaceAll("(^-|-$)", "");
        return normalized.isBlank() ? "reporte-dinamico" : normalized;
    }

    private String csv(String value) {
        String escaped = safe(value).replace("\"", "\"\"");
        return "\"" + escaped + "\"";
    }

    private String html(String value) {
        return safe(value)
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;");
    }

    private String pdf(String value) {
        return safe(value).replace("\\", "\\\\").replace("(", "\\(").replace(")", "\\)");
    }

    private String shorten(String value, int max) {
        if (value == null || value.length() <= max) {
            return safe(value);
        }
        return value.substring(0, Math.max(0, max - 3)) + "...";
    }
}
