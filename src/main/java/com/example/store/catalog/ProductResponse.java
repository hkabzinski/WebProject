package com.example.store.catalog;

import com.example.store.common.Money;

public record ProductResponse(String id, String title, int available, String price) {

    public static ProductResponse of(Product product) {
        return new ProductResponse(
                String.valueOf(product.getId()),
                product.getTitle(),
                product.getAvailable(),
                Money.format(product.getPrice()));
    }
}
