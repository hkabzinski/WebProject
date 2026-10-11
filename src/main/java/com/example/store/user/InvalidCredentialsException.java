package com.example.store.user;

import com.example.store.common.ApiException;
import org.springframework.http.HttpStatus;

public class InvalidCredentialsException extends ApiException {

    private static final String MESSAGE = "Email or password is incorrect";

    public InvalidCredentialsException() {
        super(HttpStatus.UNAUTHORIZED, MESSAGE);
    }
}
