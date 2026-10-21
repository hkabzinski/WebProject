package com.example.store.cart;

import com.example.store.common.Money;
import com.example.store.common.Ordinals;
import java.math.BigDecimal;
import java.util.List;

public record CartResponse(List<CartItemResponse> items, String total) {

    public static CartResponse of(Cart cart) {
        return new CartResponse(
                Ordinals.assign(cart.getItems(), CartItemResponse::of),
                Money.format(cart.calculateTotal()));
    }

    public static CartResponse empty() {
        return new CartResponse(List.of(), Money.format(BigDecimal.ZERO));
    }
}
