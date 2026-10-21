package com.example.store.cart;

import com.example.store.common.ApiException;
import org.springframework.http.HttpStatus;

public class EmptyCartException extends ApiException {

    private static final String MESSAGE = "Cart is empty";

    public EmptyCartException() {
        super(HttpStatus.BAD_REQUEST, MESSAGE);
    }
}
