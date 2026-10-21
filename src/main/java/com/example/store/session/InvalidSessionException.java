package com.example.store.session;

import com.example.store.common.ApiException;
import org.springframework.http.HttpStatus;

public class InvalidSessionException extends ApiException {

    private InvalidSessionException(String message) {
        super(HttpStatus.UNAUTHORIZED, message);
    }

    public static InvalidSessionException missingHeader(String headerName) {
        return new InvalidSessionException("Request is missing the %s header".formatted(headerName));
    }

    public static InvalidSessionException notActive(String sessionId) {
        return new InvalidSessionException("Session %s is unknown or has expired".formatted(sessionId));
    }
}
