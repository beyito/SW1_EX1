package com.politicanegocio.core.dto;

public record DocumentPermissionUpdateRequest(
        String userId,
        boolean canView,
        boolean canEdit,
        boolean canDelete
) {
}
