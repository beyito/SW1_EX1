package com.politicanegocio.core.model;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Data
@Document(collection = "documents")
public class DocumentRecord {
    @Id
    private String id;
    private String processInstanceId;
    private String policyId;
    private String clientId;
    private String documentCode;
    private String fileName;
    private String contentType;
    private long size;
    private String s3Key;
    private String createdBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
