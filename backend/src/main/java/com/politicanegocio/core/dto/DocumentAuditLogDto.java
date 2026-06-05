package com.politicanegocio.core.dto;

import com.politicanegocio.core.model.DocumentAction;
import com.politicanegocio.core.model.DocumentAuditLog;

import java.time.LocalDateTime;

public record DocumentAuditLogDto(
        String id,
        String username,
        String userId,
        DocumentAction action,
        String documentId,
        String processInstanceId,
        String httpMethod,
        String path,
        LocalDateTime timestamp
) {
    public static DocumentAuditLogDto from(DocumentAuditLog log) {
        return new DocumentAuditLogDto(
                log.getId(),
                log.getUsername(),
                log.getUserId(),
                log.getAction(),
                log.getDocumentId(),
                log.getProcessInstanceId(),
                log.getHttpMethod(),
                log.getPath(),
                log.getTimestamp()
        );
    }
}
