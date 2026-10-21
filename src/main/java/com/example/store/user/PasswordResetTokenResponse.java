package com.example.store.user;

import java.time.Instant;

public record PasswordResetTokenResponse(String resetToken, Instant expiresAt) {

    public static PasswordResetTokenResponse of(PasswordResetToken token) {
        return new PasswordResetTokenResponse(token.getId(), token.getExpiresAt());
    }
}
