package com.example.store.session;

import com.example.store.common.StoreProperties;
import com.example.store.user.User;
import java.time.Clock;
import java.time.Instant;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class DefaultSessionService implements SessionService {

    private final UserSessionRepository userSessionRepository;
    private final StoreProperties storeProperties;
    private final Clock clock;

    public DefaultSessionService(UserSessionRepository userSessionRepository,
                                 StoreProperties storeProperties,
                                 Clock clock) {
        this.userSessionRepository = userSessionRepository;
        this.storeProperties = storeProperties;
        this.clock = clock;
    }

    @Override
    public UserSession openSession(User user) {
        Instant now = clock.instant();
        UserSession session = new UserSession(
                UUID.randomUUID().toString(),
                user,
                now,
                now.plus(storeProperties.session().timeToLive()));
        return userSessionRepository.save(session);
    }

    @Override
    @Transactional(readOnly = true)
    public UserSession requireActiveSession(String sessionId) {
        UserSession session = userSessionRepository.findById(sessionId)
                .orElseThrow(() -> InvalidSessionException.notActive(sessionId));
        if (session.isExpiredAt(clock.instant())) {
            throw InvalidSessionException.notActive(sessionId);
        }
        return session;
    }
}
