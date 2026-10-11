package com.example.store.user;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import com.example.store.session.SessionService;
import com.example.store.session.UserSession;
import com.example.store.support.TestFixtures;
import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class DefaultAuthenticationServiceTest {

    private static final long USER_ID = 11L;
    private static final String EMAIL = "my@email.com";
    private static final String RAW_PASSWORD = "123";
    private static final String WRONG_PASSWORD = "wrong";
    private static final String PASSWORD_HASH = "$2a$10$encoded";
    private static final String SESSION_ID = "5f1d9b2c-0000-4000-8000-000000000001";
    private static final Instant NOW = Instant.parse("2026-01-01T10:00:00Z");
    private static final Instant SESSION_EXPIRY = Instant.parse("2026-01-01T10:30:00Z");

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private LoginAttemptPolicy loginAttemptPolicy;

    @Mock
    private SessionService sessionService;

    @InjectMocks
    private DefaultAuthenticationService authenticationService;

    @Test
    void login_WhenCredentialsAreValid_OpensSessionForUser() {
        User user = TestFixtures.user(USER_ID, EMAIL, PASSWORD_HASH);
        UserSession expectedSession = TestFixtures.session(SESSION_ID, user, NOW, SESSION_EXPIRY);
        given(userRepository.findByEmail(EMAIL)).willReturn(Optional.of(user));
        given(passwordEncoder.matches(RAW_PASSWORD, PASSWORD_HASH)).willReturn(true);
        given(sessionService.openSession(user)).willReturn(expectedSession);

        UserSession session = authenticationService.login(EMAIL, RAW_PASSWORD);

        assertThat(session.getId()).isEqualTo(SESSION_ID);
        verify(loginAttemptPolicy).registerSuccessfulAttempt(user);
    }

    @Test
    void login_WhenEmailIsUnknown_ThrowsInvalidCredentialsException() {
        given(userRepository.findByEmail(EMAIL)).willReturn(Optional.empty());

        assertThatThrownBy(() -> authenticationService.login(EMAIL, RAW_PASSWORD))
                .isInstanceOf(InvalidCredentialsException.class);

        verifyNoInteractions(sessionService);
    }

    @Test
    void login_WhenPasswordDoesNotMatch_RegistersFailedAttemptAndThrowsInvalidCredentialsException() {
        User user = TestFixtures.user(USER_ID, EMAIL, PASSWORD_HASH);
        given(userRepository.findByEmail(EMAIL)).willReturn(Optional.of(user));
        given(passwordEncoder.matches(WRONG_PASSWORD, PASSWORD_HASH)).willReturn(false);

        assertThatThrownBy(() -> authenticationService.login(EMAIL, WRONG_PASSWORD))
                .isInstanceOf(InvalidCredentialsException.class);

        verify(loginAttemptPolicy).registerFailedAttempt(user);
        verify(loginAttemptPolicy, never()).registerSuccessfulAttempt(user);
        verifyNoInteractions(sessionService);
    }

    @Test
    void login_WhenAccountIsLocked_ThrowsAccountLockedExceptionWithoutCheckingPassword() {
        User user = TestFixtures.user(USER_ID, EMAIL, PASSWORD_HASH);
        given(userRepository.findByEmail(EMAIL)).willReturn(Optional.of(user));
        willThrow(new AccountLockedException(SESSION_EXPIRY)).given(loginAttemptPolicy).verifyNotLocked(user);

        assertThatThrownBy(() -> authenticationService.login(EMAIL, RAW_PASSWORD))
                .isInstanceOf(AccountLockedException.class);

        verifyNoInteractions(passwordEncoder, sessionService);
    }
}
