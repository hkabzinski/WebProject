package com.example.store.order;

import com.example.store.common.Money;
import com.example.store.common.Ordinals;
import java.time.Instant;
import java.util.List;

public record OrderResponse(
        String id,
        Instant createdAt,
        String total,
        OrderStatus status,
        List<OrderItemResponse> items) {

    public static OrderResponse of(Order order) {
        return new OrderResponse(
                String.valueOf(order.getId()),
                order.getCreatedAt(),
                Money.format(order.calculateTotal()),
                order.getStatus(),
                Ordinals.assign(order.getItems(), OrderItemResponse::of));
    }
}
