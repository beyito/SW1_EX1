package com.politicanegocio.core.repository;

import com.politicanegocio.core.model.DocumentPermission;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.Optional;

public interface DocumentPermissionRepository extends MongoRepository<DocumentPermission, String> {
    List<DocumentPermission> findByDocumentIdOrderByUsernameAsc(String documentId);

    Optional<DocumentPermission> findByDocumentIdAndUserId(String documentId, String userId);

    void deleteByDocumentId(String documentId);
}
