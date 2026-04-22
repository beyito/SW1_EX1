package com.politicanegocio.core.service;

import com.politicanegocio.core.model.User;
import com.politicanegocio.core.repository.UserRepository;

import com.politicanegocio.core.security.TokenService;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.UNAUTHORIZED;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final TokenService tokenService;

    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder, TokenService tokenService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.tokenService = tokenService;
    }

    public User registerUser(String username, String rawPassword, String role, String company, String parentCompany) {
        userRepository.findByUsername(username).ifPresent(existing -> {
            throw new ResponseStatusException(BAD_REQUEST, "El nombre de usuario ya existe");
        });

        List<String> validRoles = List.of("SOFTWARE_ADMIN", "COMPANY_ADMIN", "FUNCTIONARY");
        if (!validRoles.contains(role)) {
            throw new ResponseStatusException(BAD_REQUEST, "Rol inválido");
        }

        if ("SOFTWARE_ADMIN".equals(role) && userRepository.existsByRolesContaining("SOFTWARE_ADMIN")) {
            throw new ResponseStatusException(BAD_REQUEST, "Ya existe un admin de software registrado");
        }

        if (!"FUNCTIONARY".equals(role) && !"SOFTWARE_ADMIN".equals(role)) {
            throw new ResponseStatusException(BAD_REQUEST, "Solo se permiten funcionarios y un primer admin de software desde este registro");
        }

        User user = new User();
        user.setUsername(username);
        user.setPassword(passwordEncoder.encode(rawPassword));
        user.setRoles(List.of(role));
        user.setCompany(company);
        user.setParentCompany(parentCompany);
        return userRepository.save(user);
    }

    public boolean softwareAdminExists() {
        return userRepository.existsByRolesContaining("SOFTWARE_ADMIN");
    }

    public String login(String username, String password) {
        
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResponseStatusException(UNAUTHORIZED, "Credenciales inválidas"));
        if (!passwordEncoder.matches(password, user.getPassword())) {
            throw new ResponseStatusException(UNAUTHORIZED, "Credenciales inválidas");
        }

        return tokenService.createTokenForUser(user);
    }

    public User getByUsername(String username) {
        return userRepository.findByUsername(username).orElse(null);
    }

    public User getUserByToken(String token) {
        return tokenService.findUserByToken(token);
    }
}
