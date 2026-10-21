package com.example.store.cart;

import com.example.store.session.UserSession;

public interface CartService {

    CartResponse addPosition(UserSession session, Long productId, int quantity);

    CartResponse viewCart(UserSession session);

    CartResponse changeQuantity(UserSession session, Long itemId, int quantity);

    void removeItem(UserSession session, Long itemId);

    Cart requireNonEmptyCart(UserSession session);
}
