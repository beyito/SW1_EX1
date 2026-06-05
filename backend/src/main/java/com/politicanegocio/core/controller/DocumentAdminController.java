package com.politicanegocio.core.controller;

import com.politicanegocio.core.dto.AdminDocumentDto;
import com.politicanegocio.core.dto.DocumentAuditLogDto;
import com.politicanegocio.core.dto.DocumentPermissionDto;
import com.politicanegocio.core.dto.DocumentPermissionUpdateRequest;
import com.politicanegocio.core.model.User;
import com.politicanegocio.core.service.DocumentAdminService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/admin/documents")
@PreAuthorize("hasAuthority('COMPANY_ADMIN')")
public class DocumentAdminController {
    private final DocumentAdminService documentAdminService;

    public DocumentAdminController(DocumentAdminService documentAdminService) {
        this.documentAdminService = documentAdminService;
    }

    @GetMapping
    public ResponseEntity<List<AdminDocumentDto>> listDocuments(Authentication authentication) {
        return ResponseEntity.ok(documentAdminService.listCompanyDocuments(currentUser(authentication)));
    }

    @GetMapping("/audit")
    public ResponseEntity<List<DocumentAuditLogDto>> listCompanyAudit(Authentication authentication) {
        return ResponseEntity.ok(documentAdminService.listCompanyAudit(currentUser(authentication)));
    }

    @GetMapping("/{documentId}/permissions")
    public ResponseEntity<List<DocumentPermissionDto>> listPermissions(
            @PathVariable String documentId,
            Authentication authentication
    ) {
        return ResponseEntity.ok(documentAdminService.listPermissions(documentId, currentUser(authentication)));
    }

    @PutMapping("/{documentId}/permissions")
    public ResponseEntity<DocumentPermissionDto> savePermission(
            @PathVariable String documentId,
            @RequestBody DocumentPermissionUpdateRequest request,
            Authentication authentication
    ) {
        return ResponseEntity.ok(documentAdminService.savePermission(documentId, request, currentUser(authentication)));
    }

    @GetMapping("/{documentId}/audit")
    public ResponseEntity<List<DocumentAuditLogDto>> listDocumentAudit(
            @PathVariable String documentId,
            Authentication authentication
    ) {
        return ResponseEntity.ok(documentAdminService.listDocumentAudit(documentId, currentUser(authentication)));
    }

    private User currentUser(Authentication authentication) {
        if (authentication == null || !(authentication.getPrincipal() instanceof User user)) {
            throw new IllegalStateException("Usuario no autenticado");
        }
        return user;
    }
}
