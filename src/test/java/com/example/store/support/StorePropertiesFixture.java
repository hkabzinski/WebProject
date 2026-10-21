package com.example.store.support;

import com.example.store.common.StoreProperties;
import java.time.Duration;

public final class StorePropertiesFixture {

    public static final Duration SESSION_TIME_TO_LIVE = Duration.ofMinutes(30);
    public static final int MAX_FAILED_ATTEMPTS = 5;
    public static final Duration LOCK_DURATION = Duration.ofMinutes(15);
    public static final Duration PASSWORD_RESET_TIME_TO_LIVE = Duration.ofMinutes(30);

    private StorePropertiesFixture() {
    }

    public static StoreProperties storeProperties() {
        return new StoreProperties(
                new StoreProperties.Session(SESSION_TIME_TO_LIVE),
                new StoreProperties.Login(MAX_FAILED_ATTEMPTS, LOCK_DURATION),
                new StoreProperties.PasswordReset(PASSWORD_RESET_TIME_TO_LIVE));
    }
}
