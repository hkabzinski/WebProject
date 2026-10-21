package com.example.store.user;

import com.example.store.common.StoreProperties;
import java.time.Clock;
import java.time.Instant;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Transactional
public class DefaultLoginAttemptPolicy implements LoginAttemptPolicy {

    private final UserRepository userRepository;
    private final StoreProperties storeProperties;
    private final Clock clock;

    public DefaultLoginAttemptPolicy(UserRepository userRepository, StoreProperties storeProperties, Clock clock) {
        this.userRepository = userRepository;
        this.storeProperties = storeProperties;
        this.clock = clock;
    }

    @Override
    public void verifyNotLocked(User user) {
        if (user.isLockedAt(clock.instant())) {
            throw new AccountLockedException(user.getLockedUntil());
        }
        if (user.hasLockRecord()) {
            clearAttempts(user);
        }
    }

    @Override
    public void registerFailedAttempt(User user) {
        user.registerFailedLoginAttempt();
        if (user.getFailedLoginAttempts() >= storeProperties.login().maxFailedAttempts()) {
            user.lockUntil(clock.instant().plus(storeProperties.login().lockDuration()));
        }
        userRepository.save(user);
    }

    @Override
    public void registerSuccessfulAttempt(User user) {
        if (user.getFailedLoginAttempts() > 0 || user.hasLockRecord()) {
            clearAttempts(user);
        }
    }

    private void clearAttempts(User user) {
        user.clearFailedLoginAttempts();
        userRepository.save(user);
    }
}
