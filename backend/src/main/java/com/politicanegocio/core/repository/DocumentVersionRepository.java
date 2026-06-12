package com.politicanegocio.core.repository;

import com.politicanegocio.core.model.DocumentVersion;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.Optional;

public interface DocumentVersionRepository extends MongoRepository<DocumentVersion, String> {
    List<DocumentVersion> findByDocumentIdOrderByVersionNumberDesc(String documentId);

    Optional<DocumentVersion> findByDocumentIdAndVersionNumber(String documentId, int versionNumber);

    void deleteByDocumentId(String documentId);
}
