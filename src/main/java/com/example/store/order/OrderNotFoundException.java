package com.example.store.order;

import com.example.store.common.ApiException;
import org.springframework.http.HttpStatus;

public class OrderNotFoundException extends ApiException {

    public OrderNotFoundException(Long orderId) {
        super(HttpStatus.NOT_FOUND, "Order %d does not exist".formatted(orderId));
    }
}
