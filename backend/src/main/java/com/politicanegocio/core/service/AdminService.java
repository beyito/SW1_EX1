package com.politicanegocio.core.service;

import com.politicanegocio.core.model.Area;
import com.politicanegocio.core.model.Company;
import com.politicanegocio.core.model.User;
import com.politicanegocio.core.repository.AreaRepository;
import com.politicanegocio.core.repository.CompanyRepository;
import com.politicanegocio.core.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

import static org.springframework.http.HttpStatus.BAD_REQUEST;

@Service
public class AdminService {

    private final UserRepository userRepository;
    private final CompanyRepository companyRepository;
    private final AreaRepository areaRepository;
    private final PasswordEncoder passwordEncoder;

    public AdminService(UserRepository userRepository, CompanyRepository companyRepository, AreaRepository areaRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.companyRepository = companyRepository;
        this.areaRepository = areaRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public Company createCompany(String companyName, User creator) {
        if (companyName == null || companyName.isBlank()) {
            throw new ResponseStatusException(BAD_REQUEST, "El nombre de la empresa es requerido");
        }
        if (companyRepository.findByName(companyName).isPresent()) {
            throw new ResponseStatusException(BAD_REQUEST, "La empresa ya existe");
        }

        Company company = new Company();
        company.setName(companyName.trim());
        company.setCreatedBy(creator.getUsername());
        return companyRepository.save(company);
    }

    public User createCompanyAdmin(String username, String rawPassword, String companyName, User creator) {
        if (username == null || username.isBlank() || rawPassword == null || rawPassword.isBlank()) {
            throw new ResponseStatusException(BAD_REQUEST, "Usuario y contraseña son obligatorios");
        }
        Company company = companyRepository.findByName(companyName)
                .orElseThrow(() -> new ResponseStatusException(BAD_REQUEST, "La empresa no existe"));

        userRepository.findByUsername(username).ifPresent(existing -> {
            throw new ResponseStatusException(BAD_REQUEST, "El nombre de usuario ya existe");
        });

        User user = new User();
        user.setUsername(username.trim());
        user.setPassword(passwordEncoder.encode(rawPassword));
        user.setRoles(List.of("COMPANY_ADMIN"));
        user.setCompany(company.getName());
        user.setParentCompany(company.getName());
        return userRepository.save(user);
    }

    public Area createArea(String name, String company, List<String> streets, User creator) {
        if (name == null || name.isBlank()) {
            throw new ResponseStatusException(BAD_REQUEST, "El nombre del área es obligatorio");
        }
        if (areaRepository.findByNameAndCompany(name.trim(), company).isPresent()) {
            throw new ResponseStatusException(BAD_REQUEST, "El área ya existe en esta empresa");
        }

        Area area = new Area();
        area.setName(name.trim());
        area.setCompany(company);
        area.setStreets(streets != null ? streets.stream().map(String::trim).filter(s -> !s.isBlank()).toList() : List.of());
        return areaRepository.save(area);
    }

    public User createFunctionary(String username, String rawPassword, String company, String areaName, User creator) {
        if (username == null || username.isBlank() || rawPassword == null || rawPassword.isBlank() || areaName == null || areaName.isBlank()) {
            throw new ResponseStatusException(BAD_REQUEST, "Usuario, contraseña y área son obligatorios");
        }

        areaRepository.findByNameAndCompany(areaName.trim(), company)
                .orElseThrow(() -> new ResponseStatusException(BAD_REQUEST, "El área no existe en esta empresa"));

        userRepository.findByUsername(username).ifPresent(existing -> {
            throw new ResponseStatusException(BAD_REQUEST, "El nombre de usuario ya existe");
        });

        User user = new User();
        user.setUsername(username.trim());
        user.setPassword(passwordEncoder.encode(rawPassword));
        user.setRoles(List.of("FUNCTIONARY"));
        user.setCompany(company);
        user.setParentCompany(company);
        user.setArea(areaName.trim());
        user.setLaneId(areaName.trim());
        return userRepository.save(user);
    }

    public List<Company> listCompanies() {
        return companyRepository.findAll();
    }

    public List<Area> listCompanyAreas(String company) {
        return areaRepository.findByCompany(company);
    }

    public List<User> listCompanyFunctionaries(String company) {
        return userRepository.findByRolesContainingAndCompany("FUNCTIONARY", company);
    }
}
