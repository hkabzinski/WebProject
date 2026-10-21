package com.example.store.cart;

import com.example.store.session.AuthenticatedSession;
import com.example.store.session.UserSession;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/cart")
public class CartController {

    private final CartService cartService;

    public CartController(CartService cartService) {
        this.cartService = cartService;
    }

    @PostMapping
    public ResponseEntity<CartResponse> addPosition(@AuthenticatedSession UserSession session,
                                                    @Valid @RequestBody AddCartPositionRequest request) {
        CartResponse cart = cartService.addPosition(session, request.id(), request.quantity());
        return ResponseEntity.status(HttpStatus.CREATED).body(cart);
    }

    @GetMapping
    public ResponseEntity<CartResponse> viewCart(@AuthenticatedSession UserSession session) {
        return ResponseEntity.ok(cartService.viewCart(session));
    }

    @PatchMapping("/items/{itemId}")
    public ResponseEntity<CartResponse> changeQuantity(@AuthenticatedSession UserSession session,
                                                       @PathVariable Long itemId,
                                                       @Valid @RequestBody ChangeQuantityRequest request) {
        return ResponseEntity.ok(cartService.changeQuantity(session, itemId, request.quantity()));
    }

    @DeleteMapping("/items/{itemId}")
    public ResponseEntity<Void> removeItem(@AuthenticatedSession UserSession session, @PathVariable Long itemId) {
        cartService.removeItem(session, itemId);
        return ResponseEntity.noContent().build();
    }
}
