package com.example.store.common;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "store")
public record StoreProperties(Session session, Login login, PasswordReset passwordReset) {

    public record Session(Duration timeToLive) {
    }

    public record Login(int maxFailedAttempts, Duration lockDuration) {
    }

    public record PasswordReset(Duration timeToLive) {
    }
}
