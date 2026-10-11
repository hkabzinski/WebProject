package com.example.store.order;

import com.example.store.common.ApiException;
import org.springframework.http.HttpStatus;

public class OrderNotCancellableException extends ApiException {

    public OrderNotCancellableException(Long orderId, OrderStatus status) {
        super(HttpStatus.CONFLICT, "Order %d cannot be cancelled while it is %s".formatted(orderId, status));
    }
}
