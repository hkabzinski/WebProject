package com.example.store.user;

public interface PasswordResetService {

    PasswordResetTokenResponse requestPasswordReset(String email);

    void resetPassword(String token, String newRawPassword);
}
