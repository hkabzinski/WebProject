package com.example.store.user;

import com.example.store.common.ApiException;
import org.springframework.http.HttpStatus;

public class InvalidPasswordResetTokenException extends ApiException {

    private static final String MESSAGE = "Password reset token is unknown, already used or expired";

    public InvalidPasswordResetTokenException() {
        super(HttpStatus.BAD_REQUEST, MESSAGE);
    }
}
