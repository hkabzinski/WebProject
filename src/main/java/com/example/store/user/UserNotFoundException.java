package com.example.store.user;

import com.example.store.common.ApiException;
import org.springframework.http.HttpStatus;

public class UserNotFoundException extends ApiException {

    public UserNotFoundException(String email) {
        super(HttpStatus.NOT_FOUND, "No user is registered with email %s".formatted(email));
    }
}
