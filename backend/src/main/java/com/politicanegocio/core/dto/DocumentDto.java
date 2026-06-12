package com.politicanegocio.core.dto;

import com.politicanegocio.core.model.DocumentRecord;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DocumentDto {
    private String id;
    private String processInstanceId;
    private String documentCode;
    private String fileName;
    private String contentType;
    private long size;
    private String url;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private int currentVersionNumber;
    private String currentS3VersionId;
    private boolean canEdit;
    private boolean canDelete;

    public static DocumentDto from(DocumentRecord record, String url) {
        return from(record, url, false, false);
    }

    public static DocumentDto from(DocumentRecord record, String url, boolean canEdit, boolean canDelete) {
        return new DocumentDto(
                record.getId(),
                record.getProcessInstanceId(),
                record.getDocumentCode(),
                record.getFileName(),
                record.getContentType(),
                record.getSize(),
                url,
                record.getCreatedAt(),
                record.getUpdatedAt(),
                record.getCurrentVersionNumber(),
                record.getCurrentS3VersionId(),
                canEdit,
                canDelete
        );
    }
}
