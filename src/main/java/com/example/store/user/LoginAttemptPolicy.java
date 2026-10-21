package com.example.store.user;

public interface LoginAttemptPolicy {

    void verifyNotLocked(User user);

    void registerFailedAttempt(User user);

    void registerSuccessfulAttempt(User user);
}
