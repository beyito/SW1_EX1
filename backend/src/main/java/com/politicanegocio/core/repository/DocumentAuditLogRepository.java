package com.politicanegocio.core.repository;

import com.politicanegocio.core.model.DocumentAuditLog;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface DocumentAuditLogRepository extends MongoRepository<DocumentAuditLog, String> {
    List<DocumentAuditLog> findByDocumentIdOrderByTimestampDesc(String documentId);

    List<DocumentAuditLog> findByDocumentIdInOrderByTimestampDesc(List<String> documentIds);
}
