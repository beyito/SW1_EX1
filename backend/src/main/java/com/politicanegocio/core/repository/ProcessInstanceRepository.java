package com.politicanegocio.core.repository;

import com.politicanegocio.core.model.ProcessInstance;
import com.politicanegocio.core.model.ProcessInstanceStatus;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface ProcessInstanceRepository extends MongoRepository<ProcessInstance, String> {
    List<ProcessInstance> findByPolicyId(String policyId);
    List<ProcessInstance> findByStartedBy(String startedBy);
    List<ProcessInstance> findByStartedByOrderByStartedAtDesc(String startedBy);
    List<ProcessInstance> findByStatusOrderByStartedAtDesc(ProcessInstanceStatus status);
}
