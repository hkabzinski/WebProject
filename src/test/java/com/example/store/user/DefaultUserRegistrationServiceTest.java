package com.example.store.user;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.example.store.support.TestFixtures;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class DefaultUserRegistrationServiceTest {

    private static final long USER_ID = 7L;
    private static final String EMAIL = "my@email.com";
    private static final String UNNORMALIZED_EMAIL = "  MY@Email.com ";
    private static final String RAW_PASSWORD = "123";
    private static final String PASSWORD_HASH = "$2a$10$encoded";

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private DefaultUserRegistrationService userRegistrationService;

    @Captor
    private ArgumentCaptor<User> userCaptor;

    @Test
    void registerUser_WhenEmailIsNotTaken_StoresUserWithEncodedPassword() {
        given(userRepository.existsByEmail(EMAIL)).willReturn(false);
        given(passwordEncoder.encode(RAW_PASSWORD)).willReturn(PASSWORD_HASH);
        given(userRepository.save(any(User.class))).willReturn(TestFixtures.user(USER_ID, EMAIL, PASSWORD_HASH));

        UserResponse response = userRegistrationService.registerUser(EMAIL, RAW_PASSWORD);

        verify(userRepository).save(userCaptor.capture());
        assertThat(userCaptor.getValue().getPasswordHash()).isEqualTo(PASSWORD_HASH);
        assertThat(userCaptor.getValue().getPasswordHash()).isNotEqualTo(RAW_PASSWORD);
        assertThat(response).isEqualTo(new UserResponse(String.valueOf(USER_ID), EMAIL));
    }

    @Test
    void registerUser_WhenEmailIsAlreadyRegistered_ThrowsEmailAlreadyRegisteredException() {
        given(userRepository.existsByEmail(EMAIL)).willReturn(true);

        assertThatThrownBy(() -> userRegistrationService.registerUser(EMAIL, RAW_PASSWORD))
                .isInstanceOf(EmailAlreadyRegisteredException.class);

        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void registerUser_WhenEmailHasMixedCaseAndWhitespace_StoresNormalizedEmail() {
        given(userRepository.existsByEmail(EMAIL)).willReturn(false);
        given(passwordEncoder.encode(RAW_PASSWORD)).willReturn(PASSWORD_HASH);
        given(userRepository.save(any(User.class))).willReturn(TestFixtures.user(USER_ID, EMAIL, PASSWORD_HASH));

        userRegistrationService.registerUser(UNNORMALIZED_EMAIL, RAW_PASSWORD);

        verify(userRepository).save(userCaptor.capture());
        assertThat(userCaptor.getValue().getEmail()).isEqualTo(EMAIL);
    }
}
