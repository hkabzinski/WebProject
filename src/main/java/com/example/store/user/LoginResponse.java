package com.example.store.user;

import com.example.store.session.UserSession;
import java.time.Instant;

public record LoginResponse(String sessionId, Instant expiresAt) {

    public static LoginResponse of(UserSession session) {
        return new LoginResponse(session.getId(), session.getExpiresAt());
    }
}
