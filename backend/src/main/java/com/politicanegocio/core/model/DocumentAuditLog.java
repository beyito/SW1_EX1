package com.politicanegocio.core.model;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Data
@Document(collection = "document_audit_logs")
public class DocumentAuditLog {
    @Id
    private String id;
    private String username;
    private String userId;
    private DocumentAction action;
    private String documentId;
    private String processInstanceId;
    private String httpMethod;
    private String path;
    private LocalDateTime timestamp;
}
