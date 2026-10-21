package com.example.store.cart;

import com.example.store.common.Money;

public record CartItemResponse(
        int ordinal,
        String itemId,
        String productId,
        String title,
        int quantity,
        String unitPrice,
        String subtotal) {

    public static CartItemResponse of(int ordinal, CartItem item) {
        return new CartItemResponse(
                ordinal,
                String.valueOf(item.getId()),
                String.valueOf(item.getProduct().getId()),
                item.getProduct().getTitle(),
                item.getQuantity(),
                Money.format(item.getUnitPrice()),
                Money.format(item.calculateSubtotal()));
    }
}
