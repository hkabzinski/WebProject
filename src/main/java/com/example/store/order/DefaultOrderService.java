package com.example.store.order;

import com.example.store.user.User;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class DefaultOrderService implements OrderService {

    private final OrderRepository orderRepository;

    public DefaultOrderService(OrderRepository orderRepository) {
        this.orderRepository = orderRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<OrderResponse> findOrderHistory(User user) {
        return orderRepository.findByUserOrderByCreatedAtDesc(user).stream()
                .map(OrderResponse::of)
                .toList();
    }

    @Override
    public OrderResponse cancelOrder(User user, Long orderId) {
        Order order = orderRepository.findByIdAndUser(orderId, user)
                .orElseThrow(() -> new OrderNotFoundException(orderId));
        order.cancel();
        return OrderResponse.of(orderRepository.save(order));
    }
}
