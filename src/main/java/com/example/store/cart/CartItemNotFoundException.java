package com.example.store.cart;

import com.example.store.common.ApiException;
import org.springframework.http.HttpStatus;

public class CartItemNotFoundException extends ApiException {

    public CartItemNotFoundException(Long itemId) {
        super(HttpStatus.NOT_FOUND, "Cart item %d is not part of the current cart".formatted(itemId));
    }
}
