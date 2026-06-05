package com.politicanegocio.core.dto;

import com.politicanegocio.core.model.DocumentRecord;

import java.time.LocalDateTime;

public record AdminDocumentDto(
        String id,
        String processInstanceId,
        String policyId,
        String documentCode,
        String fileName,
        String contentType,
        long size,
        String createdBy,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static AdminDocumentDto from(DocumentRecord record) {
        return new AdminDocumentDto(
                record.getId(),
                record.getProcessInstanceId(),
                record.getPolicyId(),
                record.getDocumentCode(),
                record.getFileName(),
                record.getContentType(),
                record.getSize(),
                record.getCreatedBy(),
                record.getCreatedAt(),
                record.getUpdatedAt()
        );
    }
}
