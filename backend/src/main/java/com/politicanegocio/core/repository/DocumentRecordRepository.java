package com.politicanegocio.core.repository;

import com.politicanegocio.core.model.DocumentRecord;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface DocumentRecordRepository extends MongoRepository<DocumentRecord, String> {
    List<DocumentRecord> findByProcessInstanceIdOrderByCreatedAtDesc(String processInstanceId);

    List<DocumentRecord> findByPolicyIdInOrderByCreatedAtDesc(List<String> policyIds);
}
