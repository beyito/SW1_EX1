package com.politicanegocio.core.security;

import com.politicanegocio.core.model.User;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class TokenService {

    private record UserSession(User user, Instant expiration) {}

    private final Map<String, UserSession> activeTokens = new ConcurrentHashMap<>();
    private static final long TOKEN_DURATION_SECONDS = 60 * 60 * 6; // 6 horas

    public String createTokenForUser(User user) {
        String token = UUID.randomUUID().toString();
        Instant expiration = Instant.now().plusSeconds(TOKEN_DURATION_SECONDS);
        activeTokens.put(token, new UserSession(user, expiration));
        return token;
    }

    public User findUserByToken(String bearerToken) {
        if (bearerToken == null || bearerToken.isBlank()) {
            return null;
        }

        UserSession session = activeTokens.get(bearerToken);
        if (session == null || Instant.now().isAfter(session.expiration)) {
            activeTokens.remove(bearerToken);
            return null;
        }

        return session.user();
    }
}