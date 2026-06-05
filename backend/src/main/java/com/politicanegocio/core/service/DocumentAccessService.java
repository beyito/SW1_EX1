package com.politicanegocio.core.service;

import com.politicanegocio.core.model.DocumentAction;
import com.politicanegocio.core.model.DocumentPermission;
import com.politicanegocio.core.model.DocumentRecord;
import com.politicanegocio.core.model.Policy;
import com.politicanegocio.core.model.ProcessInstance;
import com.politicanegocio.core.model.User;
import com.politicanegocio.core.repository.DocumentPermissionRepository;
import com.politicanegocio.core.repository.PolicyRepository;
import com.politicanegocio.core.repository.ProcessInstanceRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.Objects;

@Service
public class DocumentAccessService {
    private static final String ROLE_CLIENT = "CLIENT";
    private static final String ROLE_COMPANY_ADMIN = "COMPANY_ADMIN";

    private final DocumentPermissionRepository permissionRepository;
    private final ProcessInstanceRepository processInstanceRepository;
    private final PolicyRepository policyRepository;

    public DocumentAccessService(
            DocumentPermissionRepository permissionRepository,
            ProcessInstanceRepository processInstanceRepository,
            PolicyRepository policyRepository
    ) {
        this.permissionRepository = permissionRepository;
        this.processInstanceRepository = processInstanceRepository;
        this.policyRepository = policyRepository;
    }

    public void assertCanAccessDocument(User user, DocumentRecord record, DocumentAction action) {
        if (user == null || record == null || action == null) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Acceso denegado al documento");
        }

        ProcessInstance process = processInstanceRepository.findById(record.getProcessInstanceId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Instancia de proceso no encontrada"));
        assertSameCompany(user, process);

        if (isCompanyAdmin(user)) {
            return;
        }

        if (isClient(user)) {
            if (Objects.equals(process.getStartedBy(), user.getUsername()) && action == DocumentAction.VIEW) {
                return;
            }
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "El cliente no tiene permiso para esta acción documental");
        }

        DocumentPermission permission = permissionRepository
                .findByDocumentIdAndUserId(record.getId(), user.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN, "No tienes privilegios sobre este documento"));

        boolean allowed = switch (action) {
            case VIEW -> permission.isCanView() || permission.isCanEdit() || permission.isCanDelete();
            case EDIT -> permission.isCanEdit();
            case DELETE -> permission.isCanDelete();
        };
        if (!allowed) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Privilegio documental insuficiente");
        }
    }

    public void assertCanAccessProcess(User user, ProcessInstance process) {
        if (user == null || process == null) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Acceso denegado al trámite");
        }
        assertSameCompany(user, process);
        if (isCompanyAdmin(user)) {
            return;
        }
        if (isClient(user) && !Objects.equals(process.getStartedBy(), user.getUsername())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Documento de otro cliente");
        }
    }

    public void assertCompanyAdmin(User user) {
        if (!isCompanyAdmin(user)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Solo el administrador de empresa puede gestionar privilegios documentales");
        }
    }

    public boolean canView(User user, DocumentRecord record) {
        try {
            assertCanAccessDocument(user, record, DocumentAction.VIEW);
            return true;
        } catch (ResponseStatusException exception) {
            return false;
        }
    }

    public boolean canEdit(User user, DocumentRecord record) {
        try {
            assertCanAccessDocument(user, record, DocumentAction.EDIT);
            return true;
        } catch (ResponseStatusException exception) {
            return false;
        }
    }

    public boolean canDelete(User user, DocumentRecord record) {
        try {
            assertCanAccessDocument(user, record, DocumentAction.DELETE);
            return true;
        } catch (ResponseStatusException exception) {
            return false;
        }
    }

    public boolean isCompanyAdmin(User user) {
        return user != null && user.getRoles() != null && user.getRoles().contains(ROLE_COMPANY_ADMIN);
    }

    private void assertSameCompany(User user, ProcessInstance process) {
        Policy policy = policyRepository.findById(process.getPolicyId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Política no encontrada"));
        if (!Objects.equals(policy.getCompanyId(), user.getCompany())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Documento de otra empresa");
        }
    }

    private boolean isClient(User user) {
        return user.getRoles() != null && user.getRoles().contains(ROLE_CLIENT);
    }
}
