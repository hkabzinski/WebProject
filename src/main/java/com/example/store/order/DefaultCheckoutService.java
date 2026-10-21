package com.example.store.order;

import com.example.store.cart.Cart;
import com.example.store.cart.CartItem;
import com.example.store.cart.CartService;
import com.example.store.catalog.Product;
import com.example.store.session.UserSession;
import java.time.Clock;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class DefaultCheckoutService implements CheckoutService {

    private final CartService cartService;
    private final OrderRepository orderRepository;
    private final Clock clock;

    public DefaultCheckoutService(CartService cartService, OrderRepository orderRepository, Clock clock) {
        this.cartService = cartService;
        this.orderRepository = orderRepository;
        this.clock = clock;
    }

    @Override
    public OrderResponse checkout(UserSession session) {
        Cart cart = cartService.requireNonEmptyCart(session);
        List<CartItem> positions = cart.getItems();
        positions.forEach(DefaultCheckoutService::verifyPurchasable);
        Order order = new Order(session.getUser(), clock.instant());
        positions.forEach(position -> reserve(order, position));
        cart.clear();
        return OrderResponse.of(orderRepository.save(order));
    }

    private static void verifyPurchasable(CartItem position) {
        Product product = position.getProduct();
        verifyPriceUnchanged(position, product);
        product.ensureAvailable(position.getQuantity());
    }

    private static void verifyPriceUnchanged(CartItem position, Product product) {
        if (position.getUnitPrice().compareTo(product.getPrice()) != 0) {
            throw new PriceChangedException(product.getTitle(), position.getUnitPrice(), product.getPrice());
        }
    }

    private static void reserve(Order order, CartItem position) {
        Product product = position.getProduct();
        product.reduceAvailable(position.getQuantity());
        order.addItem(new OrderItem(order, product, position.getQuantity(), position.getUnitPrice()));
    }
}
