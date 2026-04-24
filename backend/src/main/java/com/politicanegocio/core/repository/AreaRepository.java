package com.politicanegocio.core.repository;

import com.politicanegocio.core.model.Area;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.Optional;

public interface AreaRepository extends MongoRepository<Area, String> {
    List<Area> findByCompany(String company);
    Optional<Area> findByNameAndCompany(String name, String company);
    Optional<Area> findByNameIgnoreCaseAndCompanyIgnoreCase(String name, String company);
    Optional<Area> findByIdAndCompany(String id, String company);
}
