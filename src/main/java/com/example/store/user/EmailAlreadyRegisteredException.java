package com.example.store.user;

import com.example.store.common.ApiException;
import org.springframework.http.HttpStatus;

public class EmailAlreadyRegisteredException extends ApiException {

    public EmailAlreadyRegisteredException(String email) {
        super(HttpStatus.CONFLICT, "Email %s is already registered".formatted(email));
    }
}
