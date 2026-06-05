package com.politicanegocio.core.controller;

import com.politicanegocio.core.dto.UploadResponseDto;
import com.politicanegocio.core.model.DocumentRecord;
import com.politicanegocio.core.model.User;
import com.politicanegocio.core.service.DocumentService;
import com.politicanegocio.core.service.S3Service;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/files")
public class FileUploadController {

    private final S3Service s3Service;
    private final DocumentService documentService;

    public FileUploadController(S3Service s3Service, DocumentService documentService) {
        this.s3Service = s3Service;
        this.documentService = documentService;
    }

    @PostMapping("/upload")
    public ResponseEntity<UploadResponseDto> upload(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "processInstanceId", required = false) String processInstanceId,
            @RequestParam(value = "documentId", required = false) String documentId,
            Authentication authentication
    ) throws Exception {
        if (file.isEmpty()) {
            return ResponseEntity.badRequest().build();
        }

        User currentUser = resolveCurrentUser(authentication);
        String clientId = currentUser != null && currentUser.getId() != null
                ? currentUser.getId()
                : (currentUser != null ? currentUser.getUsername() : "anon");

        UploadResponseDto upload = s3Service.upload(file, clientId, processInstanceId, documentId);
        if (processInstanceId != null && !processInstanceId.isBlank()) {
            DocumentRecord record = documentService.registerUpload(
                    processInstanceId,
                    documentId,
                    file.getOriginalFilename(),
                    file.getContentType(),
                    file.getSize(),
                    upload.getKey(),
                    currentUser
            );
            upload.setDocumentId(record.getId());
        }

        return ResponseEntity.ok(upload);
    }

    private User resolveCurrentUser(Authentication authentication) {
        if (authentication == null || !(authentication.getPrincipal() instanceof User user)) {
            return null;
        }
        return user;
    }
}
