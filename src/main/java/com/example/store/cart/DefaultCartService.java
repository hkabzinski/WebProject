package com.example.store.cart;

import com.example.store.catalog.Product;
import com.example.store.catalog.ProductCatalogService;
import com.example.store.session.UserSession;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class DefaultCartService implements CartService {

    private final CartRepository cartRepository;
    private final ProductCatalogService productCatalogService;

    public DefaultCartService(CartRepository cartRepository, ProductCatalogService productCatalogService) {
        this.cartRepository = cartRepository;
        this.productCatalogService = productCatalogService;
    }

    @Override
    public CartResponse addPosition(UserSession session, Long productId, int quantity) {
        Product product = productCatalogService.requireProduct(productId);
        Cart cart = openCart(session);
        Optional<CartItem> existingItem = cart.findItemOf(product);
        int requestedQuantity = existingItem.map(CartItem::getQuantity).orElse(0) + quantity;
        product.ensureAvailable(requestedQuantity);
        existingItem.ifPresentOrElse(
                item -> item.update(requestedQuantity, product.getPrice()),
                () -> cart.addItem(new CartItem(cart, product, quantity, product.getPrice())));
        return CartResponse.of(cartRepository.save(cart));
    }

    @Override
    @Transactional(readOnly = true)
    public CartResponse viewCart(UserSession session) {
        return findCart(session).map(CartResponse::of).orElseGet(CartResponse::empty);
    }

    @Override
    public CartResponse changeQuantity(UserSession session, Long itemId, int quantity) {
        Cart cart = requireCart(session, itemId);
        CartItem item = cart.requireItem(itemId);
        Product product = item.getProduct();
        product.ensureAvailable(quantity);
        item.update(quantity, product.getPrice());
        return CartResponse.of(cartRepository.save(cart));
    }

    @Override
    public void removeItem(UserSession session, Long itemId) {
        Cart cart = requireCart(session, itemId);
        cart.removeItem(cart.requireItem(itemId));
        cartRepository.save(cart);
    }

    @Override
    public Cart requireNonEmptyCart(UserSession session) {
        Cart cart = findCart(session).orElseThrow(EmptyCartException::new);
        if (cart.isEmpty()) {
            throw new EmptyCartException();
        }
        return cart;
    }

    private Cart requireCart(UserSession session, Long itemId) {
        return findCart(session).orElseThrow(() -> new CartItemNotFoundException(itemId));
    }

    private Optional<Cart> findCart(UserSession session) {
        return cartRepository.findBySession(session);
    }

    private Cart openCart(UserSession session) {
        return findCart(session).orElseGet(() -> new Cart(session));
    }
}
