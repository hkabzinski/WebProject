package com.example.store.user;

import com.example.store.session.UserSession;

public interface AuthenticationService {

    UserSession login(String email, String rawPassword);
}
