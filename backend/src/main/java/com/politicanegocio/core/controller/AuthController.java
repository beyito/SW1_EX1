package com.politicanegocio.core.controller;

import com.politicanegocio.core.model.User;
import com.politicanegocio.core.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    public ResponseEntity<Map<String, String>> register(@RequestBody RegisterRequest request) {
        User user = authService.registerUser(request.username(), request.password(), request.role(), request.company(), request.parentCompany());
        return ResponseEntity.ok(Map.of("username", user.getUsername(), "company", user.getCompany()));
    }

    @PostMapping("/login")
    public ResponseEntity<Map<String, Object>> login(@RequestBody LoginRequest request) {
        String token = authService.login(request.username(), request.password());
        User user = authService.getByUsername(request.username());
        return ResponseEntity.ok(buildLoginResponse(user, token));
    }

    @PostMapping("/mobile/login")
    public ResponseEntity<Map<String, Object>> loginMobile(@RequestBody LoginRequest request) {
        String token = authService.loginForMobileClient(request.username(), request.password());
        User user = authService.getByUsername(request.username());
        return ResponseEntity.ok(buildLoginResponse(user, token));
    }

    private Map<String, Object> buildLoginResponse(User user, String token) {
        Map<String, Object> response = new HashMap<>();
        response.put("token", token);
        response.put("username", user.getUsername());
        response.put("roles", user.getRoles());
        response.put("company", Objects.toString(user.getCompany(), ""));
        response.put("parentCompany", Objects.toString(user.getParentCompany(), ""));
        response.put("area", Objects.toString(user.getArea(), ""));
        response.put("laneId", resolveLaneId(user));
        return response;
    }

    @GetMapping("/me")
    public ResponseEntity<Map<String, Object>> me(Authentication authentication) {
        if (authentication == null || !(authentication.getPrincipal() instanceof User user)) {
            return ResponseEntity.status(401).build();
        }
        Map<String, Object> response = new HashMap<>();
        response.put("username", user.getUsername());
        response.put("roles", user.getRoles());
        response.put("company", Objects.toString(user.getCompany(), ""));
        response.put("parentCompany", Objects.toString(user.getParentCompany(), ""));
        response.put("area", Objects.toString(user.getArea(), ""));
        response.put("laneId", resolveLaneId(user));
        return ResponseEntity.ok(response);
    }

    private String resolveLaneId(User user) {
        if (user.getLaneId() != null && !user.getLaneId().isBlank()) {
            return user.getLaneId();
        }
        return Objects.toString(user.getArea(), "");
    }

    private record LoginRequest(String username, String password) {}
    private record RegisterRequest(String username, String password, String role, String company, String parentCompany) {}
}
