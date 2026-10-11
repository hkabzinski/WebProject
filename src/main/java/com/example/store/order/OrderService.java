package com.example.store.order;

import com.example.store.user.User;
import java.util.List;

public interface OrderService {

    List<OrderResponse> findOrderHistory(User user);

    OrderResponse cancelOrder(User user, Long orderId);
}
