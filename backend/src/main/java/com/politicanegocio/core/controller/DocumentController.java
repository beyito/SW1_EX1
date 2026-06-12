package com.politicanegocio.core.controller;

import com.politicanegocio.core.dto.DocumentDto;
import com.politicanegocio.core.dto.DocumentVersionDto;
import com.politicanegocio.core.model.User;
import com.politicanegocio.core.service.DocumentService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/documents")
public class DocumentController {
    private final DocumentService documentService;

    public DocumentController(DocumentService documentService) {
        this.documentService = documentService;
    }

    @GetMapping("/{documentId}")
    public ResponseEntity<DocumentDto> getDocument(@PathVariable String documentId, Authentication authentication) {
        return ResponseEntity.ok(documentService.getDocument(documentId, currentUser(authentication)));
    }

    @GetMapping("/process/{processInstanceId}")
    public ResponseEntity<List<DocumentDto>> getProcessDocuments(
            @PathVariable String processInstanceId,
            Authentication authentication
    ) {
        return ResponseEntity.ok(documentService.getDocumentsForProcess(processInstanceId, currentUser(authentication)));
    }

    @GetMapping("/{documentId}/versions")
    public ResponseEntity<List<DocumentVersionDto>> getDocumentVersions(
            @PathVariable String documentId,
            Authentication authentication
    ) {
        return ResponseEntity.ok(documentService.listVersions(documentId, currentUser(authentication)));
    }

    @PostMapping("/{documentId}/versions/{versionNumber}/restore")
    public ResponseEntity<DocumentDto> restoreDocumentVersion(
            @PathVariable String documentId,
            @PathVariable int versionNumber,
            Authentication authentication
    ) {
        return ResponseEntity.ok(documentService.restoreVersion(documentId, versionNumber, currentUser(authentication)));
    }

    @DeleteMapping("/{documentId}")
    public ResponseEntity<Void> deleteDocument(@PathVariable String documentId, Authentication authentication) {
        documentService.deleteDocument(documentId, currentUser(authentication));
        return ResponseEntity.noContent().build();
    }

    private User currentUser(Authentication authentication) {
        if (authentication == null || !(authentication.getPrincipal() instanceof User user)) {
            return null;
        }
        return user;
    }
}
