package com.politicanegocio.core.model;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Data
@Document(collection = "document_versions")
@CompoundIndex(name = "document_version_unique", def = "{'documentId': 1, 'versionNumber': 1}", unique = true)
public class DocumentVersion {
    @Id
    private String id;
    private String documentId;
    private String processInstanceId;
    private String policyId;
    private int versionNumber;
    private String fileName;
    private String contentType;
    private long size;
    private String s3Key;
    private String s3VersionId;
    private String createdBy;
    private String source;
    private LocalDateTime createdAt;
}
