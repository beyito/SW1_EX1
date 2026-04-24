package com.politicanegocio.core.repository;

import com.politicanegocio.core.model.User;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends MongoRepository<User, String> {
    Optional<User> findByUsername(String username);
    Optional<User> findByUsernameIgnoreCase(String username);
    boolean existsByRolesContaining(String role);
    List<User> findByCompany(String company);
    List<User> findByRolesContainingAndCompany(String role, String company);
    List<User> findByRolesContaining(String role);
    Optional<User> findByIdAndCompany(String id, String company);
}
