package com.politicanegocio.core.controller;

import com.politicanegocio.core.model.User;
import com.politicanegocio.core.repository.UserRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.Map;

@RestController
@RequestMapping("/api/notifications")
public class NotificationTokenController {
    private final UserRepository userRepository;

    public NotificationTokenController(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @PostMapping("/device-token")
    public ResponseEntity<Map<String, String>> registerDeviceToken(
            @RequestBody DeviceTokenRequest request,
            Authentication authentication
    ) {
        User actor = (User) authentication.getPrincipal();
        String token = request == null || request.token() == null ? "" : request.token().trim();

        if (token.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("message", "El token FCM es obligatorio."));
        }

        User persistedUser = userRepository.findById(actor.getId()).orElse(actor);
        persistedUser.setFcmToken(token);
        persistedUser.setFcmTokenUpdatedAt(LocalDateTime.now());
        userRepository.save(persistedUser);

        return ResponseEntity.ok(Map.of("message", "Token FCM registrado correctamente."));
    }

    private record DeviceTokenRequest(String token) {
    }
}
