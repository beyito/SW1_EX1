package com.politicanegocio.core.dto;

import com.politicanegocio.core.model.DocumentVersion;

import java.time.LocalDateTime;

public record DocumentVersionDto(
        String id,
        String documentId,
        int versionNumber,
        String fileName,
        String contentType,
        long size,
        String s3VersionId,
        String createdBy,
        String source,
        LocalDateTime createdAt,
        boolean restorable
) {
    public static DocumentVersionDto from(DocumentVersion version) {
        return new DocumentVersionDto(
                version.getId(),
                version.getDocumentId(),
                version.getVersionNumber(),
                version.getFileName(),
                version.getContentType(),
                version.getSize(),
                version.getS3VersionId(),
                version.getCreatedBy(),
                version.getSource(),
                version.getCreatedAt(),
                version.getS3VersionId() != null && !version.getS3VersionId().isBlank()
        );
    }
}
