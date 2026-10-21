package com.example.store.order;

import com.example.store.session.AuthenticatedSession;
import com.example.store.session.UserSession;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/orders")
public class OrderController {

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @GetMapping
    public ResponseEntity<List<OrderResponse>> listOrders(@AuthenticatedSession UserSession session) {
        return ResponseEntity.ok(orderService.findOrderHistory(session.getUser()));
    }

    @PostMapping("/{orderId}/cancel")
    public ResponseEntity<OrderResponse> cancelOrder(@AuthenticatedSession UserSession session,
                                                     @PathVariable Long orderId) {
        return ResponseEntity.ok(orderService.cancelOrder(session.getUser(), orderId));
    }
}
