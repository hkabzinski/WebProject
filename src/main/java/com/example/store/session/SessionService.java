package com.example.store.session;

import com.example.store.user.User;

public interface SessionService {

    UserSession openSession(User user);

    UserSession requireActiveSession(String sessionId);
}
