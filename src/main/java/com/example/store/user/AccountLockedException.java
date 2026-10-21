package com.example.store.user;

import com.example.store.common.ApiException;
import java.time.Instant;
import org.springframework.http.HttpStatus;

public class AccountLockedException extends ApiException {

    public AccountLockedException(Instant lockedUntil) {
        super(HttpStatus.LOCKED,
                "Account is locked after too many failed login attempts, retry after %s".formatted(lockedUntil));
    }
}
