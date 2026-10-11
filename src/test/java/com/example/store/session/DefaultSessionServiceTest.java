package com.example.store.session;

import static com.example.store.support.StorePropertiesFixture.SESSION_TIME_TO_LIVE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

import com.example.store.support.StorePropertiesFixture;
import com.example.store.support.TestFixtures;
import com.example.store.user.User;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DefaultSessionServiceTest {

    private static final long USER_ID = 5L;
    private static final String EMAIL = "my@email.com";
    private static final String PASSWORD_HASH = "$2a$10$encoded";
    private static final String SESSION_ID = "99999999-8888-7777-6666-555555555555";
    private static final String UNKNOWN_SESSION_ID = "00000000-0000-0000-0000-000000000000";
    private static final Instant NOW = Instant.parse("2026-01-01T10:00:00Z");
    private static final Instant STILL_VALID = NOW.plusSeconds(60);
    private static final Instant ALREADY_EXPIRED = NOW.minusSeconds(1);

    @Mock
    private UserSessionRepository userSessionRepository;

    private DefaultSessionService sessionService;

    private User user;

    @BeforeEach
    void createService() {
        sessionService = new DefaultSessionService(
                userSessionRepository,
                StorePropertiesFixture.storeProperties(),
                Clock.fixed(NOW, ZoneOffset.UTC));
        user = TestFixtures.user(USER_ID, EMAIL, PASSWORD_HASH);
    }

    @Test
    void openSession_ReturnsSessionExpiringAfterConfiguredTimeToLive() {
        given(userSessionRepository.save(any(UserSession.class)))
                .willAnswer(invocation -> invocation.getArgument(0));

        UserSession session = sessionService.openSession(user);

        assertThat(session.getId()).isNotBlank();
        assertThat(session.getUser()).isEqualTo(user);
        assertThat(session.getCreatedAt()).isEqualTo(NOW);
        assertThat(session.getExpiresAt()).isEqualTo(NOW.plus(SESSION_TIME_TO_LIVE));
    }

    @Test
    void openSession_WhenCalledTwice_ReturnsDistinctSessionIdentifiers() {
        given(userSessionRepository.save(any(UserSession.class)))
                .willAnswer(invocation -> invocation.getArgument(0));

        UserSession firstSession = sessionService.openSession(user);
        UserSession secondSession = sessionService.openSession(user);

        assertThat(firstSession.getId()).isNotEqualTo(secondSession.getId());
    }

    @Test
    void requireActiveSession_WhenSessionIsActive_ReturnsSession() {
        UserSession stored = TestFixtures.session(SESSION_ID, user, NOW, STILL_VALID);
        given(userSessionRepository.findById(SESSION_ID)).willReturn(Optional.of(stored));

        UserSession session = sessionService.requireActiveSession(SESSION_ID);

        assertThat(session).isEqualTo(stored);
    }

    @Test
    void requireActiveSession_WhenSessionIsUnknown_ThrowsInvalidSessionException() {
        given(userSessionRepository.findById(UNKNOWN_SESSION_ID)).willReturn(Optional.empty());

        assertThatThrownBy(() -> sessionService.requireActiveSession(UNKNOWN_SESSION_ID))
                .isInstanceOf(InvalidSessionException.class);
    }

    @Test
    void requireActiveSession_WhenSessionHasExpired_ThrowsInvalidSessionException() {
        UserSession expired = TestFixtures.session(SESSION_ID, user, ALREADY_EXPIRED, ALREADY_EXPIRED);
        given(userSessionRepository.findById(SESSION_ID)).willReturn(Optional.of(expired));

        assertThatThrownBy(() -> sessionService.requireActiveSession(SESSION_ID))
                .isInstanceOf(InvalidSessionException.class);
    }
}
