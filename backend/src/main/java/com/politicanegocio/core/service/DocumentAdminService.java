package com.politicanegocio.core.service;

import com.politicanegocio.core.dto.AdminDocumentDto;
import com.politicanegocio.core.dto.DocumentAuditLogDto;
import com.politicanegocio.core.dto.DocumentPermissionDto;
import com.politicanegocio.core.dto.DocumentPermissionUpdateRequest;
import com.politicanegocio.core.model.DocumentPermission;
import com.politicanegocio.core.model.DocumentRecord;
import com.politicanegocio.core.model.Policy;
import com.politicanegocio.core.model.User;
import com.politicanegocio.core.repository.DocumentAuditLogRepository;
import com.politicanegocio.core.repository.DocumentPermissionRepository;
import com.politicanegocio.core.repository.DocumentRecordRepository;
import com.politicanegocio.core.repository.PolicyRepository;
import com.politicanegocio.core.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

@Service
public class DocumentAdminService {
    private static final String ROLE_FUNCTIONARY = "FUNCTIONARY";

    private final DocumentRecordRepository documentRecordRepository;
    private final DocumentPermissionRepository permissionRepository;
    private final DocumentAuditLogRepository auditLogRepository;
    private final PolicyRepository policyRepository;
    private final UserRepository userRepository;
    private final DocumentAccessService accessService;

    public DocumentAdminService(
            DocumentRecordRepository documentRecordRepository,
            DocumentPermissionRepository permissionRepository,
            DocumentAuditLogRepository auditLogRepository,
            PolicyRepository policyRepository,
            UserRepository userRepository,
            DocumentAccessService accessService
    ) {
        this.documentRecordRepository = documentRecordRepository;
        this.permissionRepository = permissionRepository;
        this.auditLogRepository = auditLogRepository;
        this.policyRepository = policyRepository;
        this.userRepository = userRepository;
        this.accessService = accessService;
    }

    public List<AdminDocumentDto> listCompanyDocuments(User admin) {
        accessService.assertCompanyAdmin(admin);
        List<String> policyIds = policyRepository.findByCompanyId(admin.getCompany()).stream()
                .map(Policy::getId)
                .toList();
        if (policyIds.isEmpty()) {
            return List.of();
        }
        return documentRecordRepository.findByPolicyIdInOrderByCreatedAtDesc(policyIds).stream()
                .map(AdminDocumentDto::from)
                .toList();
    }

    public List<DocumentPermissionDto> listPermissions(String documentId, User admin) {
        accessService.assertCompanyAdmin(admin);
        assertDocumentBelongsToCompany(documentId, admin.getCompany());
        return permissionRepository.findByDocumentIdOrderByUsernameAsc(documentId).stream()
                .map(DocumentPermissionDto::from)
                .toList();
    }

    public DocumentPermissionDto savePermission(String documentId, DocumentPermissionUpdateRequest request, User admin) {
        accessService.assertCompanyAdmin(admin);
        assertDocumentBelongsToCompany(documentId, admin.getCompany());
        if (request == null || !StringUtils.hasText(request.userId())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Funcionario requerido");
        }

        User functionary = userRepository.findByIdAndCompany(request.userId(), admin.getCompany())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Funcionario no encontrado"));
        if (functionary.getRoles() == null || !functionary.getRoles().contains(ROLE_FUNCTIONARY)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El usuario no es funcionario");
        }

        DocumentPermission permission = permissionRepository.findByDocumentIdAndUserId(documentId, functionary.getId())
                .orElseGet(DocumentPermission::new);
        permission.setDocumentId(documentId);
        permission.setUserId(functionary.getId());
        permission.setUsername(functionary.getUsername());
        permission.setCompany(admin.getCompany());
        permission.setCanView(request.canView() || request.canEdit() || request.canDelete());
        permission.setCanEdit(request.canEdit());
        permission.setCanDelete(request.canDelete());
        permission.setUpdatedBy(admin.getUsername());
        permission.setUpdatedAt(LocalDateTime.now());
        return DocumentPermissionDto.from(permissionRepository.save(permission));
    }

    public List<DocumentAuditLogDto> listDocumentAudit(String documentId, User admin) {
        accessService.assertCompanyAdmin(admin);
        assertDocumentBelongsToCompany(documentId, admin.getCompany());
        return auditLogRepository.findByDocumentIdOrderByTimestampDesc(documentId).stream()
                .map(DocumentAuditLogDto::from)
                .toList();
    }

    public List<DocumentAuditLogDto> listCompanyAudit(User admin) {
        accessService.assertCompanyAdmin(admin);
        List<String> documentIds = listCompanyDocuments(admin).stream()
                .map(AdminDocumentDto::id)
                .toList();
        if (documentIds.isEmpty()) {
            return List.of();
        }
        return auditLogRepository.findByDocumentIdInOrderByTimestampDesc(documentIds).stream()
                .map(DocumentAuditLogDto::from)
                .toList();
    }

    private DocumentRecord assertDocumentBelongsToCompany(String documentId, String company) {
        DocumentRecord record = documentRecordRepository.findById(documentId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Documento no encontrado"));
        Policy policy = policyRepository.findById(record.getPolicyId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Política no encontrada"));
        if (!Objects.equals(policy.getCompanyId(), company)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Documento de otra empresa");
        }
        return record;
    }
}
