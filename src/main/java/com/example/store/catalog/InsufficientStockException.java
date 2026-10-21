package com.example.store.catalog;

import com.example.store.common.ApiException;
import org.springframework.http.HttpStatus;

public class InsufficientStockException extends ApiException {

    public InsufficientStockException(String title, int available, int requested) {
        super(HttpStatus.CONFLICT,
                "Product '%s' has %d item(s) available but %d were requested".formatted(title, available, requested));
    }
}
