package com.example.store.user;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.store.session.SessionService;
import com.example.store.session.UserSession;
import com.example.store.support.TestFixtures;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(UserController.class)
class UserControllerTest {

    private static final String REGISTER_PATH = "/api/users/register";
    private static final String LOGIN_PATH = "/api/users/login";
    private static final String PASSWORD_RESET_REQUEST_PATH = "/api/users/password-reset/request";
    private static final String PASSWORD_RESET_PATH = "/api/users/password-reset";
    private static final String CREDENTIALS_TEMPLATE = """
            {"email":"%s","password":"%s"}""";
    private static final long USER_ID = 7L;
    private static final String EMAIL = "my@email.com";
    private static final String MALFORMED_EMAIL = "not-an-email";
    private static final String RAW_PASSWORD = "123";
    private static final String NEW_PASSWORD = "newSecret";
    private static final int CONFLICT_STATUS = 409;
    private static final String PASSWORD_HASH = "$2a$10$encoded";
    private static final String SESSION_ID = "aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee";
    private static final String TOKEN_ID = "11111111-2222-3333-4444-555555555555";
    private static final Instant NOW = Instant.parse("2026-01-01T10:00:00Z");
    private static final Instant SESSION_EXPIRY = Instant.parse("2026-01-01T10:30:00Z");
    private static final String CREDENTIALS_BODY = CREDENTIALS_TEMPLATE.formatted(EMAIL, RAW_PASSWORD);
    private static final String INVALID_EMAIL_BODY = CREDENTIALS_TEMPLATE.formatted(MALFORMED_EMAIL, RAW_PASSWORD);
    private static final String PASSWORD_RESET_REQUEST_BODY = """
            {"email":"%s"}""".formatted(EMAIL);
    private static final String PASSWORD_RESET_BODY = """
            {"token":"%s","newPassword":"%s"}""".formatted(TOKEN_ID, NEW_PASSWORD);

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UserRegistrationService userRegistrationService;

    @MockitoBean
    private AuthenticationService authenticationService;

    @MockitoBean
    private PasswordResetService passwordResetService;

    @MockitoBean
    private SessionService sessionService;

    @Test
    void registerUser_WhenEmailIsAvailable_ReturnsOkWithCreatedUser() throws Exception {
        given(userRegistrationService.registerUser(EMAIL, RAW_PASSWORD))
                .willReturn(new UserResponse(String.valueOf(USER_ID), EMAIL));

        mockMvc.perform(post(REGISTER_PATH).contentType(MediaType.APPLICATION_JSON).content(CREDENTIALS_BODY))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(String.valueOf(USER_ID)))
                .andExpect(jsonPath("$.email").value(EMAIL));
    }

    @Test
    void registerUser_WhenEmailIsAlreadyRegistered_ReturnsConflict() throws Exception {
        willThrow(new EmailAlreadyRegisteredException(EMAIL))
                .given(userRegistrationService).registerUser(anyString(), anyString());

        mockMvc.perform(post(REGISTER_PATH).contentType(MediaType.APPLICATION_JSON).content(CREDENTIALS_BODY))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(CONFLICT_STATUS));
    }

    @Test
    void registerUser_WhenEmailIsMalformed_ReturnsBadRequest() throws Exception {
        mockMvc.perform(post(REGISTER_PATH).contentType(MediaType.APPLICATION_JSON).content(INVALID_EMAIL_BODY))
                .andExpect(status().isBadRequest());
    }

    @Test
    void login_WhenCredentialsAreValid_ReturnsGeneratedSessionId() throws Exception {
        User user = TestFixtures.user(USER_ID, EMAIL, PASSWORD_HASH);
        UserSession session = TestFixtures.session(SESSION_ID, user, NOW, SESSION_EXPIRY);
        given(authenticationService.login(anyString(), anyString())).willReturn(session);

        mockMvc.perform(post(LOGIN_PATH).contentType(MediaType.APPLICATION_JSON).content(CREDENTIALS_BODY))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sessionId").value(SESSION_ID));
    }

    @Test
    void login_WhenCredentialsAreInvalid_ReturnsUnauthorized() throws Exception {
        willThrow(new InvalidCredentialsException())
                .given(authenticationService).login(anyString(), anyString());

        mockMvc.perform(post(LOGIN_PATH).contentType(MediaType.APPLICATION_JSON).content(CREDENTIALS_BODY))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void login_WhenAccountIsLocked_ReturnsLocked() throws Exception {
        willThrow(new AccountLockedException(SESSION_EXPIRY))
                .given(authenticationService).login(anyString(), anyString());

        mockMvc.perform(post(LOGIN_PATH).contentType(MediaType.APPLICATION_JSON).content(CREDENTIALS_BODY))
                .andExpect(status().isLocked());
    }

    @Test
    void requestPasswordReset_WhenUserExists_ReturnsResetToken() throws Exception {
        given(passwordResetService.requestPasswordReset(EMAIL))
                .willReturn(new PasswordResetTokenResponse(TOKEN_ID, SESSION_EXPIRY));

        mockMvc.perform(post(PASSWORD_RESET_REQUEST_PATH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(PASSWORD_RESET_REQUEST_BODY))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.resetToken").value(TOKEN_ID));
    }

    @Test
    void resetPassword_WhenTokenIsValid_ReturnsNoContent() throws Exception {
        mockMvc.perform(post(PASSWORD_RESET_PATH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(PASSWORD_RESET_BODY))
                .andExpect(status().isNoContent());
    }

    @Test
    void resetPassword_WhenTokenIsInvalid_ReturnsBadRequest() throws Exception {
        willThrow(new InvalidPasswordResetTokenException())
                .given(passwordResetService).resetPassword(anyString(), anyString());

        mockMvc.perform(post(PASSWORD_RESET_PATH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(PASSWORD_RESET_BODY))
                .andExpect(status().isBadRequest());
    }
}
