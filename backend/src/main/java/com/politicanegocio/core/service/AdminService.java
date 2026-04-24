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
import java.util.Objects;

import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.NOT_FOUND;

@Service
public class AdminService {
    public static final String CLIENT_AREA_NAME = "Cliente";
    public static final String CLIENT_LANE_ID = "lane_cliente";
    private static final String ROLE_FUNCTIONARY = "FUNCTIONARY";
    private static final String ROLE_CLIENT = "CLIENT";
    private static final String ROLE_COMPANY_ADMIN = "COMPANY_ADMIN";

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
        String normalizedCompanyName = normalizeRequiredValue(companyName, "El nombre de la empresa es requerido");
        if (companyRepository.findByNameIgnoreCase(normalizedCompanyName).isPresent()) {
            throw new ResponseStatusException(BAD_REQUEST, "La empresa ya existe");
        }

        Company company = new Company();
        company.setName(normalizedCompanyName);
        company.setCreatedBy(creator.getUsername());
        Company savedCompany = companyRepository.save(company);
        ensureClientAreaExists(savedCompany.getName());
        return savedCompany;
    }

    public User createCompanyAdmin(String username, String rawPassword, String companyName, User creator) {
        String normalizedUsername = normalizeRequiredValue(username, "El usuario es obligatorio");
        String normalizedCompany = normalizeRequiredValue(companyName, "La empresa es obligatoria");
        validatePassword(rawPassword);

        Company company = companyRepository.findByName(normalizedCompany)
                .orElseThrow(() -> new ResponseStatusException(BAD_REQUEST, "La empresa no existe"));

        validateUniqueUsername(normalizedUsername, null);

        User user = new User();
        user.setUsername(normalizedUsername);
        user.setPassword(passwordEncoder.encode(rawPassword));
        user.setRoles(List.of(ROLE_COMPANY_ADMIN));
        user.setCompany(company.getName());
        user.setParentCompany(company.getName());
        return userRepository.save(user);
    }

    public User updateCompanyAdmin(String userId, String username, String rawPassword, User creator) {
        User existingAdmin = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Administrador no encontrado"));
        if (!existingAdmin.getRoles().contains(ROLE_COMPANY_ADMIN)) {
            throw new ResponseStatusException(BAD_REQUEST, "El usuario no es administrador de empresa");
        }

        String normalizedUsername = normalizeRequiredValue(username, "El usuario es obligatorio");
        validateUniqueUsername(normalizedUsername, existingAdmin.getId());

        existingAdmin.setUsername(normalizedUsername);
        if (rawPassword != null && !rawPassword.isBlank()) {
            existingAdmin.setPassword(passwordEncoder.encode(rawPassword));
        }
        return userRepository.save(existingAdmin);
    }

    public void deleteCompanyAdmin(String userId, User creator) {
        User existingAdmin = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Administrador no encontrado"));
        if (!existingAdmin.getRoles().contains(ROLE_COMPANY_ADMIN)) {
            throw new ResponseStatusException(BAD_REQUEST, "El usuario no es administrador de empresa");
        }
        userRepository.delete(existingAdmin);
    }

    public Area createArea(String name, String company, User creator) {
        String normalizedName = normalizeRequiredValue(name, "El nombre del area es obligatorio");
        if (areaRepository.findByNameIgnoreCaseAndCompanyIgnoreCase(normalizedName, company).isPresent()) {
            throw new ResponseStatusException(BAD_REQUEST, "El area ya existe en esta empresa");
        }

        Area area = new Area();
        area.setName(normalizedName);
        area.setCompany(company);
        return areaRepository.save(area);
    }

    public Area updateArea(String areaId, String company, String name, User creator) {
        Area area = areaRepository.findByIdAndCompany(areaId, company)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Area no encontrada"));
        String normalizedName = normalizeRequiredValue(name, "El nombre del area es obligatorio");

        if (areaRepository.findByNameIgnoreCaseAndCompanyIgnoreCase(normalizedName, company)
                .filter(found -> !Objects.equals(found.getId(), areaId))
                .isPresent()) {
            throw new ResponseStatusException(BAD_REQUEST, "El area ya existe en esta empresa");
        }

        String previousName = area.getName();
        area.setName(normalizedName);
        Area savedArea = areaRepository.save(area);

        List<User> companyUsers = userRepository.findByCompany(company);
        List<User> usersToUpdate = companyUsers.stream()
                .filter(user -> previousName.equalsIgnoreCase(user.getArea()))
                .toList();
        for (User user : usersToUpdate) {
            user.setArea(normalizedName);
            if (CLIENT_AREA_NAME.equalsIgnoreCase(normalizedName)) {
                user.setLaneId(CLIENT_LANE_ID);
            } else {
                user.setLaneId(normalizedName);
            }
            userRepository.save(user);
        }

        return savedArea;
    }

    public void deleteArea(String areaId, String company, User creator) {
        Area area = areaRepository.findByIdAndCompany(areaId, company)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Area no encontrada"));
        if (CLIENT_AREA_NAME.equalsIgnoreCase(area.getName())) {
            throw new ResponseStatusException(BAD_REQUEST, "No se puede eliminar el area Cliente");
        }

        boolean inUse = userRepository.findByCompany(company).stream()
                .anyMatch(user -> area.getName().equalsIgnoreCase(user.getArea()));
        if (inUse) {
            throw new ResponseStatusException(BAD_REQUEST, "No se puede eliminar un area con usuarios asignados");
        }
        areaRepository.delete(area);
    }

    public User createFunctionary(String username, String rawPassword, String company, String areaName, User creator) {
        String normalizedUsername = normalizeRequiredValue(username, "El usuario es obligatorio");
        validatePassword(rawPassword);
        ensureClientAreaExists(company);
        String normalizedArea = normalizeRequiredValue(areaName, "El area del funcionario es obligatoria");
        if (CLIENT_AREA_NAME.equalsIgnoreCase(normalizedArea)) {
            throw new ResponseStatusException(BAD_REQUEST, "Un funcionario no puede pertenecer al area Cliente");
        }
        areaRepository.findByNameIgnoreCaseAndCompanyIgnoreCase(normalizedArea, company)
                .orElseThrow(() -> new ResponseStatusException(BAD_REQUEST, "El area seleccionada no existe en esta empresa"));
        validateUniqueUsername(normalizedUsername, null);

        User user = new User();
        user.setUsername(normalizedUsername);
        user.setPassword(passwordEncoder.encode(rawPassword));
        user.setRoles(List.of(ROLE_FUNCTIONARY));
        user.setCompany(company);
        user.setParentCompany(company);
        user.setArea(normalizedArea);
        user.setLaneId(normalizedArea);
        return userRepository.save(user);
    }

    public User updateFunctionary(String userId, String company, String username, String rawPassword, String areaName, User creator) {
        User functionary = userRepository.findByIdAndCompany(userId, company)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Funcionario no encontrado"));
        if (!functionary.getRoles().contains(ROLE_FUNCTIONARY)) {
            throw new ResponseStatusException(BAD_REQUEST, "El usuario no es un funcionario");
        }

        String normalizedUsername = normalizeRequiredValue(username, "El usuario es obligatorio");
        String normalizedArea = normalizeRequiredValue(areaName, "El area del funcionario es obligatoria");
        if (CLIENT_AREA_NAME.equalsIgnoreCase(normalizedArea)) {
            throw new ResponseStatusException(BAD_REQUEST, "Un funcionario no puede pertenecer al area Cliente");
        }
        areaRepository.findByNameIgnoreCaseAndCompanyIgnoreCase(normalizedArea, company)
                .orElseThrow(() -> new ResponseStatusException(BAD_REQUEST, "El area seleccionada no existe en esta empresa"));
        validateUniqueUsername(normalizedUsername, functionary.getId());
        functionary.setUsername(normalizedUsername);
        if (rawPassword != null && !rawPassword.isBlank()) {
            functionary.setPassword(passwordEncoder.encode(rawPassword));
        }
        functionary.setArea(normalizedArea);
        functionary.setLaneId(normalizedArea);
        return userRepository.save(functionary);
    }

    public void deleteFunctionary(String userId, String company, User creator) {
        User functionary = userRepository.findByIdAndCompany(userId, company)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Funcionario no encontrado"));
        if (!functionary.getRoles().contains(ROLE_FUNCTIONARY)) {
            throw new ResponseStatusException(BAD_REQUEST, "El usuario no es un funcionario");
        }
        userRepository.delete(functionary);
    }

    public List<Company> listCompanies() {
        return companyRepository.findAll();
    }

    public List<User> listCompanyAdmins() {
        return userRepository.findByRolesContaining(ROLE_COMPANY_ADMIN);
    }

    public List<Area> listCompanyAreas(String company) {
        ensureClientAreaExists(company);
        return areaRepository.findByCompany(company);
    }

    public List<User> listCompanyFunctionaries(String company) {
        return userRepository.findByRolesContainingAndCompany(ROLE_FUNCTIONARY, company);
    }

    public User createClient(String username, String rawPassword, String company, User creator) {
        String normalizedUsername = normalizeRequiredValue(username, "El usuario es obligatorio");
        validatePassword(rawPassword);
        ensureClientAreaExists(company);
        validateUniqueUsername(normalizedUsername, null);

        User user = new User();
        user.setUsername(normalizedUsername);
        user.setPassword(passwordEncoder.encode(rawPassword));
        user.setRoles(List.of(ROLE_CLIENT));
        user.setCompany(company);
        user.setParentCompany(company);
        user.setArea(CLIENT_AREA_NAME);
        user.setLaneId(CLIENT_LANE_ID);
        return userRepository.save(user);
    }

    public User updateClient(String userId, String company, String username, String rawPassword, User creator) {
        User client = userRepository.findByIdAndCompany(userId, company)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Cliente no encontrado"));
        if (!client.getRoles().contains(ROLE_CLIENT)) {
            throw new ResponseStatusException(BAD_REQUEST, "El usuario no es un cliente");
        }

        String normalizedUsername = normalizeRequiredValue(username, "El usuario es obligatorio");
        validateUniqueUsername(normalizedUsername, client.getId());
        client.setUsername(normalizedUsername);
        if (rawPassword != null && !rawPassword.isBlank()) {
            client.setPassword(passwordEncoder.encode(rawPassword));
        }
        client.setArea(CLIENT_AREA_NAME);
        client.setLaneId(CLIENT_LANE_ID);
        return userRepository.save(client);
    }

    public void deleteClient(String userId, String company, User creator) {
        User client = userRepository.findByIdAndCompany(userId, company)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Cliente no encontrado"));
        if (!client.getRoles().contains(ROLE_CLIENT)) {
            throw new ResponseStatusException(BAD_REQUEST, "El usuario no es un cliente");
        }
        userRepository.delete(client);
    }

    public List<User> listCompanyClients(String company) {
        return userRepository.findByRolesContainingAndCompany(ROLE_CLIENT, company);
    }

    private void ensureClientAreaExists(String company) {
        areaRepository.findByNameIgnoreCaseAndCompanyIgnoreCase(CLIENT_AREA_NAME, company)
                .orElseGet(() -> {
                    Area area = new Area();
                    area.setName(CLIENT_AREA_NAME);
                    area.setCompany(company);
                    return areaRepository.save(area);
                });
    }

    private void validateUniqueUsername(String username, String currentUserId) {
        userRepository.findByUsernameIgnoreCase(username).ifPresent(existing -> {
            if (!Objects.equals(existing.getId(), currentUserId)) {
                throw new ResponseStatusException(BAD_REQUEST, "El nombre de usuario ya existe");
            }
        });
    }

    private String normalizeRequiredValue(String value, String errorMessage) {
        if (value == null || value.isBlank()) {
            throw new ResponseStatusException(BAD_REQUEST, errorMessage);
        }
        return value.trim();
    }

    private void validatePassword(String rawPassword) {
        if (rawPassword == null || rawPassword.isBlank()) {
            throw new ResponseStatusException(BAD_REQUEST, "La contrasena es obligatoria");
        }
    }
}
