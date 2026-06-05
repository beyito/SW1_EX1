package com.politicanegocio.core.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.politicanegocio.core.dto.OnlyOfficeConfigDto;
import com.politicanegocio.core.model.DocumentAction;
import com.politicanegocio.core.model.DocumentAuditLog;
import com.politicanegocio.core.model.DocumentRecord;
import com.politicanegocio.core.model.ProcessInstance;
import com.politicanegocio.core.model.User;
import com.politicanegocio.core.repository.DocumentAuditLogRepository;
import com.politicanegocio.core.repository.DocumentRecordRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Service
public class OnlyOfficeService {
    private static final Logger log = LoggerFactory.getLogger(OnlyOfficeService.class);
    private static final List<String> SUPPORTED_EXTENSIONS = List.of("doc", "docx", "odt", "rtf", "txt", "xls", "xlsx", "ods", "csv");

    private final DocumentRecordRepository documentRecordRepository;
    private final DocumentAuditLogRepository auditLogRepository;
    private final S3Service s3Service;
    private final DocumentAccessService accessService;
    private final HttpClient httpClient;
    private final String documentServerUrl;
    private final String internalDocumentServerUrl;
    private final String backendPublicBaseUrl;
    private final long presignedHours;
    private final String backendInternalBaseUrl;
    public OnlyOfficeService(
            DocumentRecordRepository documentRecordRepository,
            DocumentAuditLogRepository auditLogRepository,
            S3Service s3Service,
            DocumentAccessService accessService,
            @Value("${app.onlyoffice.document-server-url:http://localhost:8082}") String documentServerUrl,
            @Value("${app.onlyoffice.internal-document-server-url:http://onlyoffice}") String internalDocumentServerUrl,
            @Value("${app.backend-public-base-url:http://localhost:8080}") String backendPublicBaseUrl,
            @Value("${app.onlyoffice.presigned-url-hours:2}") long presignedHours,
            @Value("${app.backend-internal-base-url:http://bpmn-backend:8080}") String backendInternalBaseUrl // <-- NUEVO
    ) {
        this.documentRecordRepository = documentRecordRepository;
        this.auditLogRepository = auditLogRepository;
        this.s3Service = s3Service;
        this.accessService = accessService;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(20))
                .build();
        this.documentServerUrl = trimTrailingSlash(documentServerUrl);
        this.internalDocumentServerUrl = trimTrailingSlash(internalDocumentServerUrl);
        this.backendPublicBaseUrl = trimTrailingSlash(backendPublicBaseUrl);
        this.presignedHours = Math.max(1, presignedHours);
        this.backendInternalBaseUrl = trimTrailingSlash(backendInternalBaseUrl);
    }

    public OnlyOfficeConfigDto buildEditorConfig(String documentId, User user) {
        DocumentRecord record = getRecordOrThrow(documentId);
        accessService.assertCanAccessDocument(user, record, DocumentAction.EDIT);

        String extension = extension(record.getFileName());
        if (!SUPPORTED_EXTENSIONS.contains(extension)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Formato no soportado por OnlyOffice");
        }

        String s3ReadUrl = s3Service.createPresignedReadUrl(record.getS3Key(), Duration.ofHours(presignedHours));
        String callbackUrl = backendInternalBaseUrl + "/api/onlyoffice/callback/" + record.getId();

        Map<String, Object> config = new LinkedHashMap<>();
        config.put("documentType", resolveDocumentType(extension));
        config.put("type", "desktop");
        config.put("width", "100%");
        config.put("height", "100%");
        config.put("document", Map.of(
                "fileType", extension,
                "key", buildDocumentKey(record),
                "title", record.getFileName(),
                "url", s3ReadUrl,
                "permissions", Map.of(
                        "download", true,
                        "edit", true,
                        "print", true
                )
        ));
        config.put("editorConfig", Map.of(
                "callbackUrl", callbackUrl,
                "mode", "edit",
                "lang", "es",
                "user", Map.of(
                        "id", resolveUserId(user),
                        "name", user != null ? user.getUsername() : "Usuario"
                ),
                "customization", Map.of(
                        "autosave", true,
                        "forcesave", true
                )
        ));

        logAction(user, record, "GET", "/api/onlyoffice/config/" + documentId);
        return new OnlyOfficeConfigDto(documentServerUrl, config);
    }

    public void handleSaveCallback(String documentId, JsonNode payload) {
        int status = payload == null ? 0 : payload.path("status").asInt(0);
        String editedFileUrl = payload == null ? "" : payload.path("url").asText("");

        if (status != 2 && status != 6) {
            log.debug("OnlyOffice callback ignorado: documentId={} status={}", documentId, status);
            return;
        }
        if (!StringUtils.hasText(editedFileUrl)) {
            log.warn("OnlyOffice status={} sin URL de archivo editado: documentId={}", status, documentId);
            return;
        }

        try {
            DocumentRecord record = getRecordOrThrow(documentId);
            HttpRequest request = HttpRequest.newBuilder(URI.create(resolveInternalOnlyOfficeUrl(editedFileUrl)))
                    .timeout(Duration.ofMinutes(3))
                    .GET()
                    .build();
            HttpResponse<byte[]> response = httpClient.send(request, HttpResponse.BodyHandlers.ofByteArray());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                log.warn("OnlyOffice no pudo entregar archivo final: documentId={} httpStatus={}", documentId, response.statusCode());
                return;
            }

            byte[] updatedBytes = response.body();
            s3Service.replaceObject(record.getS3Key(), updatedBytes, record.getContentType());
            record.setSize(updatedBytes.length);
            record.setUpdatedAt(LocalDateTime.now());
            documentRecordRepository.save(record);
            logOnlyOfficeSave(record, "/api/onlyoffice/callback/" + documentId);
        } catch (Exception exception) {
            log.warn("Fallo guardando callback OnlyOffice: documentId={}", documentId, exception);
        }
    }

    private void logOnlyOfficeSave(DocumentRecord record, String path) {
        DocumentAuditLog auditLog = new DocumentAuditLog();
        auditLog.setUsername("onlyoffice");
        auditLog.setAction(DocumentAction.EDIT);
        auditLog.setDocumentId(record.getId());
        auditLog.setProcessInstanceId(record.getProcessInstanceId());
        auditLog.setHttpMethod("POST");
        auditLog.setPath(path);
        auditLog.setTimestamp(LocalDateTime.now());
        auditLogRepository.save(auditLog);
    }

    private void logAction(User user, DocumentRecord record, String method, String path) {
        DocumentAuditLog auditLog = new DocumentAuditLog();
        auditLog.setUsername(user != null ? user.getUsername() : "system");
        auditLog.setUserId(user != null ? user.getId() : null);
        auditLog.setAction(DocumentAction.EDIT);
        auditLog.setDocumentId(record.getId());
        auditLog.setProcessInstanceId(record.getProcessInstanceId());
        auditLog.setHttpMethod(method);
        auditLog.setPath(path);
        auditLog.setTimestamp(LocalDateTime.now());
        auditLogRepository.save(auditLog);
    }

    private DocumentRecord getRecordOrThrow(String documentId) {
        return documentRecordRepository.findById(documentId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Documento no encontrado"));
    }

    private String resolveDocumentType(String extension) {
        if (List.of("xls", "xlsx", "ods", "csv").contains(extension)) {
            return "cell";
        }
        return "word";
    }

    private String buildDocumentKey(DocumentRecord record) {
        long version = record.getUpdatedAt() == null
                ? 0
                : java.sql.Timestamp.valueOf(record.getUpdatedAt()).getTime();
        return ("oo-" + record.getId() + "-" + version).replaceAll("[^0-9A-Za-z_.=-]", "-");
    }

    private String extension(String fileName) {
        String safe = Objects.toString(fileName, "").toLowerCase();
        int dotIndex = safe.lastIndexOf('.');
        return dotIndex >= 0 && dotIndex < safe.length() - 1 ? safe.substring(dotIndex + 1) : "";
    }

    private String resolveUserId(User user) {
        if (user == null) {
            return "anonymous";
        }
        return StringUtils.hasText(user.getId()) ? user.getId() : user.getUsername();
    }

    private String trimTrailingSlash(String value) {
        String safe = Objects.toString(value, "").trim();
        while (safe.endsWith("/")) {
            safe = safe.substring(0, safe.length() - 1);
        }
        return safe;
    }

    private String resolveInternalOnlyOfficeUrl(String url) {
        if (!StringUtils.hasText(url) || !StringUtils.hasText(internalDocumentServerUrl)) {
            return url;
        }
        try {
            URI source = URI.create(url);
            URI publicServer = URI.create(documentServerUrl);
            URI internalServer = URI.create(internalDocumentServerUrl);

            if (!Objects.equals(source.getHost(), publicServer.getHost())) {
                return url;
            }

            return new URI(
                    internalServer.getScheme(),
                    internalServer.getUserInfo(),
                    internalServer.getHost(),
                    internalServer.getPort(),
                    source.getPath(),
                    source.getQuery(),
                    source.getFragment()
            ).toString();
        } catch (Exception exception) {
            log.warn("No se pudo normalizar URL interna de OnlyOffice: {}", url, exception);
            return url;
        }
    }
}
