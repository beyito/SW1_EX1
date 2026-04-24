package com.politicanegocio.core.repository;

import com.politicanegocio.core.model.ProcessInstance;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface ProcessInstanceRepository extends MongoRepository<ProcessInstance, String> {
    List<ProcessInstance> findByStartedBy(String startedBy);
}
