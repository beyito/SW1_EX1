package com.politicanegocio.core.repository;

import com.politicanegocio.core.model.Policy;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PolicyRepository extends MongoRepository<Policy, String> {
    List<Policy> findByCompanyId(String companyId);
    List<Policy> findByCompanyIdAndStartLaneId(String companyId, String startLaneId);
}
