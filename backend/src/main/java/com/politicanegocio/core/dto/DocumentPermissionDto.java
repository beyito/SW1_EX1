package com.politicanegocio.core.dto;

import com.politicanegocio.core.model.DocumentPermission;

import java.time.LocalDateTime;

public record DocumentPermissionDto(
        String id,
        String documentId,
        String userId,
        String username,
        boolean canView,
        boolean canEdit,
        boolean canDelete,
        LocalDateTime updatedAt
) {
    public static DocumentPermissionDto from(DocumentPermission permission) {
        return new DocumentPermissionDto(
                permission.getId(),
                permission.getDocumentId(),
                permission.getUserId(),
                permission.getUsername(),
                permission.isCanView(),
                permission.isCanEdit(),
                permission.isCanDelete(),
                permission.getUpdatedAt()
        );
    }
}
