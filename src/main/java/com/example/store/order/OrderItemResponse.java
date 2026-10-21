package com.example.store.order;

import com.example.store.common.Money;

public record OrderItemResponse(
        int ordinal,
        String productId,
        String title,
        int quantity,
        String unitPrice,
        String subtotal) {

    public static OrderItemResponse of(int ordinal, OrderItem item) {
        return new OrderItemResponse(
                ordinal,
                String.valueOf(item.getProduct().getId()),
                item.getTitle(),
                item.getQuantity(),
                Money.format(item.getUnitPrice()),
                Money.format(item.calculateSubtotal()));
    }
}
