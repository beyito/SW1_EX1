package com.politicanegocio.core.model;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Data
@Document(collection = "document_permissions")
@CompoundIndex(name = "document_user_unique", def = "{'documentId': 1, 'userId': 1}", unique = true)
public class DocumentPermission {
    @Id
    private String id;
    private String documentId;
    private String userId;
    private String username;
    private String company;
    private boolean canView;
    private boolean canEdit;
    private boolean canDelete;
    private String updatedBy;
    private LocalDateTime updatedAt;
}
