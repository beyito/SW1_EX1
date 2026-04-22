package com.politicanegocio.core.repository;

import com.politicanegocio.core.model.ProcessInstance;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface ProcessInstanceRepository extends MongoRepository<ProcessInstance, String> {
}
