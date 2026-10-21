package com.example.store;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.stream.IntStream;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.RequestBuilder;

@SpringBootTest
@AutoConfigureMockMvc
class LoginLockoutIntegrationTest {

    private static final String REGISTER_PATH = "/api/users/register";
    private static final String LOGIN_PATH = "/api/users/login";
    private static final String CREDENTIALS_TEMPLATE = """
            {"email":"%s","password":"%s"}""";
    private static final String LOCKED_ACCOUNT_EMAIL = "locked@email.com";
    private static final String SURVIVING_COUNTER_EMAIL = "counter@email.com";
    private static final String PASSWORD = "123";
    private static final String WRONG_PASSWORD = "wrong";
    private static final int MAX_FAILED_ATTEMPTS = 5;
    private static final int ONE_ATTEMPT_BELOW_LIMIT = MAX_FAILED_ATTEMPTS - 1;

    @Autowired
    private MockMvc mockMvc;

    @Test
    void login_WhenFailedAttemptsReachTheLimit_LocksAccountEvenForTheCorrectPassword() throws Exception {
        registerUser(LOCKED_ACCOUNT_EMAIL);
        failLogin(LOCKED_ACCOUNT_EMAIL, MAX_FAILED_ATTEMPTS);

        mockMvc.perform(login(LOCKED_ACCOUNT_EMAIL, PASSWORD))
                .andExpect(status().isLocked());
    }

    @Test
    void login_WhenFailedAttemptsStayBelowTheLimit_StillAcceptsTheCorrectPassword() throws Exception {
        registerUser(SURVIVING_COUNTER_EMAIL);
        failLogin(SURVIVING_COUNTER_EMAIL, ONE_ATTEMPT_BELOW_LIMIT);

        mockMvc.perform(login(SURVIVING_COUNTER_EMAIL, PASSWORD))
                .andExpect(status().isOk());
    }

    private void registerUser(String email) throws Exception {
        mockMvc.perform(post(REGISTER_PATH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(CREDENTIALS_TEMPLATE.formatted(email, PASSWORD)))
                .andExpect(status().isOk());
    }

    private void failLogin(String email, int attempts) {
        IntStream.range(0, attempts).forEach(attempt -> expectRejectedLogin(email));
    }

    private void expectRejectedLogin(String email) {
        try {
            mockMvc.perform(login(email, WRONG_PASSWORD)).andExpect(status().isUnauthorized());
        } catch (Exception exception) {
            throw new IllegalStateException(exception);
        }
    }

    private static RequestBuilder login(String email, String password) {
        return post(LOGIN_PATH)
                .contentType(MediaType.APPLICATION_JSON)
                .content(CREDENTIALS_TEMPLATE.formatted(email, password));
    }
}
