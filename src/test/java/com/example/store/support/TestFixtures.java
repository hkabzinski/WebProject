package com.example.store.support;

import com.example.store.cart.Cart;
import com.example.store.cart.CartItem;
import com.example.store.catalog.Product;
import com.example.store.order.Order;
import com.example.store.order.OrderItem;
import com.example.store.session.UserSession;
import com.example.store.user.PasswordResetToken;
import com.example.store.user.User;
import java.math.BigDecimal;
import java.time.Instant;
import org.springframework.test.util.ReflectionTestUtils;

public final class TestFixtures {

    private static final String ID_FIELD = "id";

    private TestFixtures() {
    }

    public static <T> T withId(T entity, long id) {
        ReflectionTestUtils.setField(entity, ID_FIELD, id);
        return entity;
    }

    public static User user(long id, String email, String passwordHash) {
        return withId(new User(email, passwordHash), id);
    }

    public static User userWithFailedAttempts(long id, String email, String passwordHash, int failedAttempts) {
        User user = user(id, email, passwordHash);
        ReflectionTestUtils.setField(user, "failedLoginAttempts", failedAttempts);
        return user;
    }

    public static User lockedUser(long id, String email, String passwordHash, Instant lockedUntil, int attempts) {
        User user = userWithFailedAttempts(id, email, passwordHash, attempts);
        user.lockUntil(lockedUntil);
        return user;
    }

    public static Product product(long id, String title, int available, String price) {
        return withId(new Product(title, available, new BigDecimal(price)), id);
    }

    public static UserSession session(String id, User user, Instant createdAt, Instant expiresAt) {
        return new UserSession(id, user, createdAt, expiresAt);
    }

    public static Cart cart(long id, UserSession session) {
        return withId(new Cart(session), id);
    }

    public static CartItem cartItem(long id, Cart cart, Product product, int quantity) {
        return cartItem(id, cart, product, quantity, product.getPrice());
    }

    public static CartItem cartItem(long id, Cart cart, Product product, int quantity, BigDecimal unitPrice) {
        CartItem item = withId(new CartItem(cart, product, quantity, unitPrice), id);
        cart.addItem(item);
        return item;
    }

    public static Order order(long id, User user, Instant createdAt) {
        return withId(new Order(user, createdAt), id);
    }

    public static OrderItem orderItem(long id, Order order, Product product, int quantity) {
        OrderItem item = withId(new OrderItem(order, product, quantity, product.getPrice()), id);
        order.addItem(item);
        return item;
    }

    public static PasswordResetToken passwordResetToken(String id, User user, Instant expiresAt) {
        return new PasswordResetToken(id, user, expiresAt);
    }
}
