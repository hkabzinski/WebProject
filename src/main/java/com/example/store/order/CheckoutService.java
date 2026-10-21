package com.example.store.order;

import com.example.store.session.UserSession;

public interface CheckoutService {

    OrderResponse checkout(UserSession session);
}
