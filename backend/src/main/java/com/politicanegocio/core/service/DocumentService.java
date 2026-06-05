package com.politicanegocio.core.service;

import com.politicanegocio.core.dto.DocumentDto;
import com.politicanegocio.core.model.DocumentAction;
import com.politicanegocio.core.model.DocumentAuditLog;
import com.politicanegocio.core.model.DocumentPermission;
import com.politicanegocio.core.model.DocumentRecord;
import com.politicanegocio.core.model.ProcessInstance;
import com.politicanegocio.core.model.User;
import com.politicanegocio.core.repository.DocumentAuditLogRepository;
import com.politicanegocio.core.repository.DocumentPermissionRepository;
import com.politicanegocio.core.repository.DocumentRecordRepository;
import com.politicanegocio.core.repository.ProcessInstanceRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class DocumentService {
    private final DocumentRecordRepository documentRecordRepository;
    private final DocumentAuditLogRepository auditLogRepository;
    private final DocumentPermissionRepository permissionRepository;
    private final ProcessInstanceRepository processInstanceRepository;
    private final S3Service s3Service;
    private final DocumentAccessService accessService;

    public DocumentService(
            DocumentRecordRepository documentRecordRepository,
            DocumentAuditLogRepository auditLogRepository,
            DocumentPermissionRepository permissionRepository,
            ProcessInstanceRepository processInstanceRepository,
            S3Service s3Service,
            DocumentAccessService accessService
    ) {
        this.documentRecordRepository = documentRecordRepository;
        this.auditLogRepository = auditLogRepository;
        this.permissionRepository = permissionRepository;
        this.processInstanceRepository = processInstanceRepository;
        this.s3Service = s3Service;
        this.accessService = accessService;
    }

    public DocumentRecord registerUpload(
            String processInstanceId,
            String documentCode,
            String fileName,
            String contentType,
            long size,
            String s3Key,
            User user
    ) {
        ProcessInstance process = getProcessOrThrow(processInstanceId);
        accessService.assertCanAccessProcess(user, process);

        DocumentRecord record = new DocumentRecord();
        record.setId(UUID.randomUUID().toString());
        record.setProcessInstanceId(process.getId());
        record.setPolicyId(process.getPolicyId());
        record.setClientId(resolveClientId(user));
        record.setDocumentCode(StringUtils.hasText(documentCode) ? documentCode.trim() : "document");
        record.setFileName(StringUtils.hasText(fileName) ? fileName.trim() : "file.bin");
        record.setContentType(StringUtils.hasText(contentType) ? contentType : "application/octet-stream");
        record.setSize(size);
        record.setS3Key(s3Key);
        record.setCreatedBy(user != null ? user.getUsername() : "system");
        record.setCreatedAt(LocalDateTime.now());
        record.setUpdatedAt(LocalDateTime.now());
        DocumentRecord saved = documentRecordRepository.save(record);
        grantUploaderPermission(saved, user);
        return saved;
    }

    public DocumentDto getDocument(String documentId, User user) {
        DocumentRecord record = getRecordOrThrow(documentId);
        accessService.assertCanAccessDocument(user, record, DocumentAction.VIEW);
        return toDto(record, user);
    }

    public List<DocumentDto> getDocumentsForProcess(String processInstanceId, User user) {
        ProcessInstance process = getProcessOrThrow(processInstanceId);
        accessService.assertCanAccessProcess(user, process);
        return documentRecordRepository.findByProcessInstanceIdOrderByCreatedAtDesc(processInstanceId).stream()
                .filter(record -> accessService.canView(user, record))
                .map(record -> toDto(record, user))
                .toList();
    }

    public void deleteDocument(String documentId, User user) {
        DocumentRecord record = getRecordOrThrow(documentId);
        accessService.assertCanAccessDocument(user, record, DocumentAction.DELETE);
        s3Service.deleteObject(record.getS3Key());
        documentRecordRepository.delete(record);
        permissionRepository.deleteByDocumentId(documentId);
        logAction(user, DocumentAction.DELETE, record, "DELETE", "/api/documents/" + documentId);
    }

    public void logAction(User user, DocumentAction action, String documentId, String method, String path) {
        DocumentRecord record = documentRecordRepository.findById(documentId).orElse(null);
        if (record == null) {
            return;
        }
        logAction(user, action, record, method, path);
    }

    public void logAction(User user, DocumentAction action, DocumentRecord record, String method, String path) {
        DocumentAuditLog auditLog = new DocumentAuditLog();
        auditLog.setUsername(user != null ? user.getUsername() : "system");
        auditLog.setUserId(user != null ? user.getId() : null);
        auditLog.setAction(action);
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

    private ProcessInstance getProcessOrThrow(String processInstanceId) {
        return processInstanceRepository.findById(processInstanceId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Instancia de proceso no encontrada"));
    }

    private String resolveClientId(User user) {
        if (user == null) {
            return "system";
        }
        return StringUtils.hasText(user.getId()) ? user.getId() : user.getUsername();
    }

    private DocumentDto toDto(DocumentRecord record, User user) {
        return DocumentDto.from(
                record,
                s3Service.createPresignedReadUrl(record.getS3Key()),
                accessService.canEdit(user, record),
                accessService.canDelete(user, record)
        );
    }

    private void grantUploaderPermission(DocumentRecord record, User user) {
        if (user == null || accessService.isCompanyAdmin(user) || user.getRoles() == null || user.getRoles().contains("CLIENT")) {
            return;
        }

        DocumentPermission permission = new DocumentPermission();
        permission.setDocumentId(record.getId());
        permission.setUserId(user.getId());
        permission.setUsername(user.getUsername());
        permission.setCompany(user.getCompany());
        permission.setCanView(true);
        permission.setCanEdit(true);
        permission.setCanDelete(true);
        permission.setUpdatedBy("system");
        permission.setUpdatedAt(LocalDateTime.now());
        permissionRepository.save(permission);
    }
}
