package com.politicanegocio.core.service;

import com.politicanegocio.core.dto.DocumentDto;
import com.politicanegocio.core.dto.DocumentVersionDto;
import com.politicanegocio.core.model.DocumentAction;
import com.politicanegocio.core.model.DocumentAuditLog;
import com.politicanegocio.core.model.DocumentPermission;
import com.politicanegocio.core.model.DocumentRecord;
import com.politicanegocio.core.model.DocumentVersion;
import com.politicanegocio.core.model.ProcessInstance;
import com.politicanegocio.core.model.User;
import com.politicanegocio.core.repository.DocumentAuditLogRepository;
import com.politicanegocio.core.repository.DocumentPermissionRepository;
import com.politicanegocio.core.repository.DocumentRecordRepository;
import com.politicanegocio.core.repository.DocumentVersionRepository;
import com.politicanegocio.core.repository.ProcessInstanceRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;
import software.amazon.awssdk.services.s3.model.S3Exception;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class DocumentService {
    private final DocumentRecordRepository documentRecordRepository;
    private final DocumentAuditLogRepository auditLogRepository;
    private final DocumentPermissionRepository permissionRepository;
    private final DocumentVersionRepository documentVersionRepository;
    private final ProcessInstanceRepository processInstanceRepository;
    private final S3Service s3Service;
    private final DocumentAccessService accessService;

    public DocumentService(
            DocumentRecordRepository documentRecordRepository,
            DocumentAuditLogRepository auditLogRepository,
            DocumentPermissionRepository permissionRepository,
            DocumentVersionRepository documentVersionRepository,
            ProcessInstanceRepository processInstanceRepository,
            S3Service s3Service,
            DocumentAccessService accessService
    ) {
        this.documentRecordRepository = documentRecordRepository;
        this.auditLogRepository = auditLogRepository;
        this.permissionRepository = permissionRepository;
        this.documentVersionRepository = documentVersionRepository;
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
            String s3VersionId,
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
        record.setCurrentS3VersionId(s3VersionId);
        record.setCurrentVersionNumber(1);
        record.setCreatedBy(user != null ? user.getUsername() : "system");
        record.setCreatedAt(LocalDateTime.now());
        record.setUpdatedAt(LocalDateTime.now());
        DocumentRecord saved = documentRecordRepository.save(record);
        createVersion(saved, s3VersionId, user != null ? user.getUsername() : "system", "UPLOAD");
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
        documentVersionRepository.deleteByDocumentId(documentId);
        permissionRepository.deleteByDocumentId(documentId);
        logAction(user, DocumentAction.DELETE, record, "DELETE", "/api/documents/" + documentId);
    }

    public List<DocumentVersionDto> listVersions(String documentId, User user) {
        DocumentRecord record = getRecordOrThrow(documentId);
        accessService.assertCanAccessDocument(user, record, DocumentAction.VIEW);
        return documentVersionRepository.findByDocumentIdOrderByVersionNumberDesc(documentId).stream()
                .map(DocumentVersionDto::from)
                .toList();
    }

    public DocumentDto restoreVersion(String documentId, int versionNumber, User user) {
        DocumentRecord record = getRecordOrThrow(documentId);
        accessService.assertCanAccessDocument(user, record, DocumentAction.EDIT);
        DocumentVersion version = documentVersionRepository.findByDocumentIdAndVersionNumber(documentId, versionNumber)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Versión documental no encontrada"));
        if (!StringUtils.hasText(version.getS3VersionId())) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Esta versión no se puede restaurar porque el bucket S3 no tenía versionamiento activo cuando se guardó."
            );
        }

        try {
            byte[] bytes = s3Service.getObjectBytes(record.getS3Key(), version.getS3VersionId());
            String newS3VersionId = s3Service.replaceObject(record.getS3Key(), bytes, record.getContentType());
            int newVersionNumber = Math.max(record.getCurrentVersionNumber(), versionNumber) + 1;
            record.setSize(bytes.length);
            record.setCurrentVersionNumber(newVersionNumber);
            record.setCurrentS3VersionId(newS3VersionId);
            record.setUpdatedAt(LocalDateTime.now());
            DocumentRecord saved = documentRecordRepository.save(record);
            createVersion(saved, newS3VersionId, user != null ? user.getUsername() : "system", "RESTORE_FROM_V" + versionNumber);
            logAction(user, DocumentAction.EDIT, saved, "POST", "/api/documents/" + documentId + "/versions/" + versionNumber + "/restore");
            return toDto(saved, user);
        } catch (S3Exception exception) {
            if (exception.statusCode() == 403) {
                throw new ResponseStatusException(
                        HttpStatus.FORBIDDEN,
                        "AWS S3 denegó la restauración. Verifica que el usuario IAM tenga permiso s3:GetObjectVersion y s3:PutObject sobre el bucket.",
                        exception
                );
            }
            throw new ResponseStatusException(
                    HttpStatus.BAD_GATEWAY,
                    "No se pudo restaurar la versión desde S3: " + exception.awsErrorDetails().errorMessage(),
                    exception
            );
        }
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

    private DocumentVersion createVersion(DocumentRecord record, String s3VersionId, String createdBy, String source) {
        DocumentVersion version = new DocumentVersion();
        version.setDocumentId(record.getId());
        version.setProcessInstanceId(record.getProcessInstanceId());
        version.setPolicyId(record.getPolicyId());
        version.setVersionNumber(Math.max(1, record.getCurrentVersionNumber()));
        version.setFileName(record.getFileName());
        version.setContentType(record.getContentType());
        version.setSize(record.getSize());
        version.setS3Key(record.getS3Key());
        version.setS3VersionId(s3VersionId);
        version.setCreatedBy(StringUtils.hasText(createdBy) ? createdBy : "system");
        version.setSource(StringUtils.hasText(source) ? source : "SYSTEM");
        version.setCreatedAt(LocalDateTime.now());
        return documentVersionRepository.save(version);
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
