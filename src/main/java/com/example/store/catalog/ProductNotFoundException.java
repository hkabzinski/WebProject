package com.example.store.catalog;

import com.example.store.common.ApiException;
import org.springframework.http.HttpStatus;

public class ProductNotFoundException extends ApiException {

    public ProductNotFoundException(Long productId) {
        super(HttpStatus.NOT_FOUND, "Product %d does not exist".formatted(productId));
    }
}
