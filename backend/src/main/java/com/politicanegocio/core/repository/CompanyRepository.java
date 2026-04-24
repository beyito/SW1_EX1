package com.politicanegocio.core.repository;

import com.politicanegocio.core.model.Company;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

public interface CompanyRepository extends MongoRepository<Company, String> {
    Optional<Company> findByName(String name);
    Optional<Company> findByNameIgnoreCase(String name);
}
