package com.example.store.user;

import static com.example.store.support.StorePropertiesFixture.PASSWORD_RESET_TIME_TO_LIVE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.example.store.support.StorePropertiesFixture;
import com.example.store.support.TestFixtures;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class DefaultPasswordResetServiceTest {

    private static final long USER_ID = 21L;
    private static final String EMAIL = "my@email.com";
    private static final String OLD_PASSWORD_HASH = "$2a$10$old";
    private static final String NEW_RAW_PASSWORD = "newSecret";
    private static final String NEW_PASSWORD_HASH = "$2a$10$new";
    private static final String TOKEN_ID = "11111111-2222-3333-4444-555555555555";
    private static final Instant NOW = Instant.parse("2026-01-01T10:00:00Z");
    private static final Instant NOT_EXPIRED = NOW.plusSeconds(60);
    private static final Instant ALREADY_EXPIRED = NOW.minusSeconds(60);

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordResetTokenRepository passwordResetTokenRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    private DefaultPasswordResetService passwordResetService;

    @BeforeEach
    void createService() {
        passwordResetService = new DefaultPasswordResetService(
                userRepository,
                passwordResetTokenRepository,
                passwordEncoder,
                StorePropertiesFixture.storeProperties(),
                Clock.fixed(NOW, ZoneOffset.UTC));
    }

    @Test
    void requestPasswordReset_WhenUserExists_ReturnsTokenExpiringAfterConfiguredTimeToLive() {
        User user = TestFixtures.user(USER_ID, EMAIL, OLD_PASSWORD_HASH);
        given(userRepository.findByEmail(EMAIL)).willReturn(Optional.of(user));
        given(passwordResetTokenRepository.save(any(PasswordResetToken.class)))
                .willAnswer(invocation -> invocation.getArgument(0));

        PasswordResetTokenResponse response = passwordResetService.requestPasswordReset(EMAIL);

        assertThat(response.resetToken()).isNotBlank();
        assertThat(response.expiresAt()).isEqualTo(NOW.plus(PASSWORD_RESET_TIME_TO_LIVE));
    }

    @Test
    void requestPasswordReset_WhenEmailIsUnknown_ThrowsUserNotFoundException() {
        given(userRepository.findByEmail(EMAIL)).willReturn(Optional.empty());

        assertThatThrownBy(() -> passwordResetService.requestPasswordReset(EMAIL))
                .isInstanceOf(UserNotFoundException.class);
    }

    @Test
    void resetPassword_WhenTokenIsValid_StoresEncodedPasswordAndConsumesToken() {
        User user = TestFixtures.user(USER_ID, EMAIL, OLD_PASSWORD_HASH);
        PasswordResetToken token = TestFixtures.passwordResetToken(TOKEN_ID, user, NOT_EXPIRED);
        given(passwordResetTokenRepository.findById(TOKEN_ID)).willReturn(Optional.of(token));
        given(passwordEncoder.encode(NEW_RAW_PASSWORD)).willReturn(NEW_PASSWORD_HASH);

        passwordResetService.resetPassword(TOKEN_ID, NEW_RAW_PASSWORD);

        assertThat(user.getPasswordHash()).isEqualTo(NEW_PASSWORD_HASH);
        assertThat(token.isUsed()).isTrue();
        verify(userRepository).save(user);
    }

    @Test
    void resetPassword_WhenTokenIsUnknown_ThrowsInvalidPasswordResetTokenException() {
        given(passwordResetTokenRepository.findById(TOKEN_ID)).willReturn(Optional.empty());

        assertThatThrownBy(() -> passwordResetService.resetPassword(TOKEN_ID, NEW_RAW_PASSWORD))
                .isInstanceOf(InvalidPasswordResetTokenException.class);

        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void resetPassword_WhenTokenHasExpired_ThrowsInvalidPasswordResetTokenException() {
        User user = TestFixtures.user(USER_ID, EMAIL, OLD_PASSWORD_HASH);
        PasswordResetToken token = TestFixtures.passwordResetToken(TOKEN_ID, user, ALREADY_EXPIRED);
        given(passwordResetTokenRepository.findById(TOKEN_ID)).willReturn(Optional.of(token));

        assertThatThrownBy(() -> passwordResetService.resetPassword(TOKEN_ID, NEW_RAW_PASSWORD))
                .isInstanceOf(InvalidPasswordResetTokenException.class);

        assertThat(user.getPasswordHash()).isEqualTo(OLD_PASSWORD_HASH);
    }

    @Test
    void resetPassword_WhenTokenWasAlreadyUsed_ThrowsInvalidPasswordResetTokenException() {
        User user = TestFixtures.user(USER_ID, EMAIL, OLD_PASSWORD_HASH);
        PasswordResetToken token = TestFixtures.passwordResetToken(TOKEN_ID, user, NOT_EXPIRED);
        token.markUsed();
        given(passwordResetTokenRepository.findById(TOKEN_ID)).willReturn(Optional.of(token));

        assertThatThrownBy(() -> passwordResetService.resetPassword(TOKEN_ID, NEW_RAW_PASSWORD))
                .isInstanceOf(InvalidPasswordResetTokenException.class);
    }
}
