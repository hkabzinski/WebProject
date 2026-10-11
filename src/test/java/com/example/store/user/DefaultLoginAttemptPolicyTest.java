package com.example.store.user;

import static com.example.store.support.StorePropertiesFixture.LOCK_DURATION;
import static com.example.store.support.StorePropertiesFixture.MAX_FAILED_ATTEMPTS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;

import com.example.store.support.StorePropertiesFixture;
import com.example.store.support.TestFixtures;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DefaultLoginAttemptPolicyTest {

    private static final long USER_ID = 3L;
    private static final String EMAIL = "my@email.com";
    private static final String PASSWORD_HASH = "$2a$10$encoded";
    private static final Instant NOW = Instant.parse("2026-01-01T10:00:00Z");
    private static final Instant ACTIVE_LOCK_EXPIRY = NOW.plusSeconds(60);
    private static final Instant ELAPSED_LOCK_EXPIRY = NOW.minusSeconds(60);
    private static final int FIRST_ATTEMPT = 1;
    private static final int ONE_ATTEMPT_BELOW_LIMIT = MAX_FAILED_ATTEMPTS - 1;
    private static final int NO_ATTEMPTS = 0;

    @Mock
    private UserRepository userRepository;

    private DefaultLoginAttemptPolicy loginAttemptPolicy;

    @BeforeEach
    void createPolicy() {
        loginAttemptPolicy = new DefaultLoginAttemptPolicy(
                userRepository,
                StorePropertiesFixture.storeProperties(),
                Clock.fixed(NOW, ZoneOffset.UTC));
    }

    @Test
    void registerFailedAttempt_WhenLimitIsNotReached_KeepsAccountUnlocked() {
        User user = TestFixtures.user(USER_ID, EMAIL, PASSWORD_HASH);

        loginAttemptPolicy.registerFailedAttempt(user);

        assertThat(user.getFailedLoginAttempts()).isEqualTo(FIRST_ATTEMPT);
        assertThat(user.isLockedAt(NOW)).isFalse();
        verify(userRepository).save(user);
    }

    @Test
    void registerFailedAttempt_WhenLimitIsReached_LocksAccountForConfiguredDuration() {
        User user = TestFixtures.userWithFailedAttempts(USER_ID, EMAIL, PASSWORD_HASH, ONE_ATTEMPT_BELOW_LIMIT);

        loginAttemptPolicy.registerFailedAttempt(user);

        assertThat(user.getFailedLoginAttempts()).isEqualTo(MAX_FAILED_ATTEMPTS);
        assertThat(user.getLockedUntil()).isEqualTo(NOW.plus(LOCK_DURATION));
        verify(userRepository).save(user);
    }

    @Test
    void verifyNotLocked_WhenLockIsStillActive_ThrowsAccountLockedException() {
        User user = TestFixtures.lockedUser(USER_ID, EMAIL, PASSWORD_HASH, ACTIVE_LOCK_EXPIRY, MAX_FAILED_ATTEMPTS);

        assertThatThrownBy(() -> loginAttemptPolicy.verifyNotLocked(user))
                .isInstanceOf(AccountLockedException.class);
    }

    @Test
    void verifyNotLocked_WhenLockHasElapsed_ClearsFailedAttempts() {
        User user = TestFixtures.lockedUser(USER_ID, EMAIL, PASSWORD_HASH, ELAPSED_LOCK_EXPIRY, MAX_FAILED_ATTEMPTS);

        loginAttemptPolicy.verifyNotLocked(user);

        assertThat(user.getFailedLoginAttempts()).isEqualTo(NO_ATTEMPTS);
        assertThat(user.getLockedUntil()).isNull();
        verify(userRepository).save(user);
    }

    @Test
    void registerSuccessfulAttempt_WhenFailedAttemptsWereRecorded_ClearsThem() {
        User user = TestFixtures.userWithFailedAttempts(USER_ID, EMAIL, PASSWORD_HASH, ONE_ATTEMPT_BELOW_LIMIT);

        loginAttemptPolicy.registerSuccessfulAttempt(user);

        assertThat(user.getFailedLoginAttempts()).isEqualTo(NO_ATTEMPTS);
        verify(userRepository).save(user);
    }
}
