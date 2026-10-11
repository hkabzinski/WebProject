package com.example.store.order;

import com.example.store.session.AuthenticatedSession;
import com.example.store.session.UserSession;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/checkout")
public class CheckoutController {

    private final CheckoutService checkoutService;

    public CheckoutController(CheckoutService checkoutService) {
        this.checkoutService = checkoutService;
    }

    @PostMapping
    public ResponseEntity<OrderResponse> checkout(@AuthenticatedSession UserSession session) {
        return ResponseEntity.status(HttpStatus.CREATED).body(checkoutService.checkout(session));
    }
}
