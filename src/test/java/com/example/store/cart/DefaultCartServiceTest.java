package com.example.store.cart;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.assertj.core.groups.Tuple.tuple;
import static org.mockito.Mockito.verify;

import com.example.store.catalog.InsufficientStockException;
import com.example.store.catalog.Product;
import com.example.store.catalog.ProductCatalogService;
import com.example.store.session.UserSession;
import com.example.store.support.TestFixtures;
import com.example.store.user.User;
import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DefaultCartServiceTest {

    private static final long USER_ID = 1L;
    private static final long CART_ID = 50L;
    private static final long NAIL_GUN_ID = 2411L;
    private static final long HAMMER_ID = 364L;
    private static final long NAIL_GUN_ITEM_ID = 100L;
    private static final long HAMMER_ITEM_ID = 101L;
    private static final long UNKNOWN_ITEM_ID = 999L;
    private static final String NAIL_GUN_TITLE = "Nail gun";
    private static final String HAMMER_TITLE = "Claw hammer";
    private static final String NAIL_GUN_PRICE = "23.95";
    private static final String HAMMER_PRICE = "12.75";
    private static final int NAIL_GUN_STOCK = 8;
    private static final int HAMMER_STOCK = 42;
    private static final int TWO_ITEMS = 2;
    private static final int THREE_ITEMS = 3;
    private static final int FIVE_ITEMS = 5;
    private static final int MORE_THAN_STOCK = NAIL_GUN_STOCK + 1;
    private static final String SESSION_ID = "aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee";
    private static final String EMAIL = "my@email.com";
    private static final String PASSWORD_HASH = "$2a$10$encoded";
    private static final Instant NOW = Instant.parse("2026-01-01T10:00:00Z");
    private static final String TWO_NAIL_GUNS_SUBTOTAL = "47.90";
    private static final String THREE_NAIL_GUNS_SUBTOTAL = "71.85";
    private static final String THREE_HAMMERS_SUBTOTAL = "38.25";
    private static final String MIXED_CART_TOTAL = "86.15";
    private static final String EMPTY_CART_TOTAL = "0.00";
    private static final int FIRST_ORDINAL = 1;
    private static final int SECOND_ORDINAL = 2;

    @Mock
    private CartRepository cartRepository;

    @Mock
    private ProductCatalogService productCatalogService;

    @InjectMocks
    private DefaultCartService cartService;

    private UserSession session;
    private Product nailGun;

    @BeforeEach
    void createSessionAndProduct() {
        User user = TestFixtures.user(USER_ID, EMAIL, PASSWORD_HASH);
        session = TestFixtures.session(SESSION_ID, user, NOW, NOW.plusSeconds(1800));
        nailGun = TestFixtures.product(NAIL_GUN_ID, NAIL_GUN_TITLE, NAIL_GUN_STOCK, NAIL_GUN_PRICE);
    }

    @Test
    void addPosition_WhenStockIsSufficient_AddsPositionWithCurrentPrice() {
        Cart cart = TestFixtures.cart(CART_ID, session);
        givenCartExists(cart);
        givenCartIsSaved();
        given(productCatalogService.requireProduct(NAIL_GUN_ID)).willReturn(nailGun);

        CartResponse response = cartService.addPosition(session, NAIL_GUN_ID, TWO_ITEMS);

        assertThat(response.items()).singleElement()
                .returns(NAIL_GUN_TITLE, CartItemResponse::title)
                .returns(TWO_ITEMS, CartItemResponse::quantity)
                .returns(NAIL_GUN_PRICE, CartItemResponse::unitPrice)
                .returns(TWO_NAIL_GUNS_SUBTOTAL, CartItemResponse::subtotal);
        assertThat(response.total()).isEqualTo(TWO_NAIL_GUNS_SUBTOTAL);
    }

    @Test
    void addPosition_WhenSessionHasNoCartYet_CreatesCartForSession() {
        givenCartIsMissing();
        givenCartIsSaved();
        given(productCatalogService.requireProduct(NAIL_GUN_ID)).willReturn(nailGun);

        CartResponse response = cartService.addPosition(session, NAIL_GUN_ID, TWO_ITEMS);

        assertThat(response.items()).hasSize(1);
        verify(cartRepository).save(any(Cart.class));
    }

    @Test
    void addPosition_WhenStockIsInsufficient_ThrowsInsufficientStockException() {
        Cart cart = TestFixtures.cart(CART_ID, session);
        givenCartExists(cart);
        given(productCatalogService.requireProduct(NAIL_GUN_ID)).willReturn(nailGun);

        assertThatThrownBy(() -> cartService.addPosition(session, NAIL_GUN_ID, MORE_THAN_STOCK))
                .isInstanceOf(InsufficientStockException.class);

        assertThat(cart.isEmpty()).isTrue();
        verify(cartRepository, never()).save(any(Cart.class));
    }

    @Test
    void addPosition_WhenProductIsAlreadyInCart_IncreasesQuantityOfSamePosition() {
        Cart cart = TestFixtures.cart(CART_ID, session);
        TestFixtures.cartItem(NAIL_GUN_ITEM_ID, cart, nailGun, TWO_ITEMS);
        givenCartExists(cart);
        givenCartIsSaved();
        given(productCatalogService.requireProduct(NAIL_GUN_ID)).willReturn(nailGun);

        CartResponse response = cartService.addPosition(session, NAIL_GUN_ID, THREE_ITEMS);

        assertThat(response.items()).singleElement()
                .returns(FIVE_ITEMS, CartItemResponse::quantity);
    }

    @Test
    void addPosition_WhenCombinedQuantityExceedsStock_ThrowsInsufficientStockException() {
        Cart cart = TestFixtures.cart(CART_ID, session);
        TestFixtures.cartItem(NAIL_GUN_ITEM_ID, cart, nailGun, NAIL_GUN_STOCK);
        givenCartExists(cart);
        given(productCatalogService.requireProduct(NAIL_GUN_ID)).willReturn(nailGun);

        assertThatThrownBy(() -> cartService.addPosition(session, NAIL_GUN_ID, TWO_ITEMS))
                .isInstanceOf(InsufficientStockException.class);
    }

    @Test
    void viewCart_WhenSessionHasNoCart_ReturnsEmptyCart() {
        givenCartIsMissing();

        CartResponse response = cartService.viewCart(session);

        assertThat(response.items()).isEmpty();
        assertThat(response.total()).isEqualTo(EMPTY_CART_TOTAL);
    }

    @Test
    void viewCart_WhenCartHasPositions_AssignsOrdinalsAndSubtotals() {
        Product hammer = TestFixtures.product(HAMMER_ID, HAMMER_TITLE, HAMMER_STOCK, HAMMER_PRICE);
        Cart cart = TestFixtures.cart(CART_ID, session);
        TestFixtures.cartItem(NAIL_GUN_ITEM_ID, cart, nailGun, TWO_ITEMS);
        TestFixtures.cartItem(HAMMER_ITEM_ID, cart, hammer, THREE_ITEMS);
        givenCartExists(cart);

        CartResponse response = cartService.viewCart(session);

        assertThat(response.items()).extracting(CartItemResponse::ordinal, CartItemResponse::title,
                        CartItemResponse::quantity, CartItemResponse::subtotal)
                .containsExactly(
                        tuple(FIRST_ORDINAL, NAIL_GUN_TITLE, TWO_ITEMS, TWO_NAIL_GUNS_SUBTOTAL),
                        tuple(SECOND_ORDINAL, HAMMER_TITLE, THREE_ITEMS, THREE_HAMMERS_SUBTOTAL));
        assertThat(response.total()).isEqualTo(MIXED_CART_TOTAL);
    }

    @Test
    void changeQuantity_WhenStockIsSufficient_UpdatesQuantityAndTotal() {
        Cart cart = TestFixtures.cart(CART_ID, session);
        TestFixtures.cartItem(NAIL_GUN_ITEM_ID, cart, nailGun, TWO_ITEMS);
        givenCartExists(cart);
        givenCartIsSaved();

        CartResponse response = cartService.changeQuantity(session, NAIL_GUN_ITEM_ID, THREE_ITEMS);

        assertThat(response.items()).singleElement()
                .returns(THREE_ITEMS, CartItemResponse::quantity)
                .returns(THREE_NAIL_GUNS_SUBTOTAL, CartItemResponse::subtotal);
        assertThat(response.total()).isEqualTo(THREE_NAIL_GUNS_SUBTOTAL);
    }

    @Test
    void changeQuantity_WhenStockIsInsufficient_ThrowsInsufficientStockException() {
        Cart cart = TestFixtures.cart(CART_ID, session);
        TestFixtures.cartItem(NAIL_GUN_ITEM_ID, cart, nailGun, TWO_ITEMS);
        givenCartExists(cart);

        assertThatThrownBy(() -> cartService.changeQuantity(session, NAIL_GUN_ITEM_ID, MORE_THAN_STOCK))
                .isInstanceOf(InsufficientStockException.class);

        assertThat(cart.getItems()).singleElement()
                .returns(TWO_ITEMS, CartItem::getQuantity);
    }

    @Test
    void changeQuantity_WhenItemIsNotInCart_ThrowsCartItemNotFoundException() {
        Cart cart = TestFixtures.cart(CART_ID, session);
        givenCartExists(cart);

        assertThatThrownBy(() -> cartService.changeQuantity(session, UNKNOWN_ITEM_ID, THREE_ITEMS))
                .isInstanceOf(CartItemNotFoundException.class);
    }

    @Test
    void removeItem_WhenItemExists_RemovesItFromCart() {
        Cart cart = TestFixtures.cart(CART_ID, session);
        TestFixtures.cartItem(NAIL_GUN_ITEM_ID, cart, nailGun, TWO_ITEMS);
        givenCartExists(cart);

        cartService.removeItem(session, NAIL_GUN_ITEM_ID);

        assertThat(cart.isEmpty()).isTrue();
        verify(cartRepository).save(cart);
    }

    @Test
    void removeItem_WhenSessionHasNoCart_ThrowsCartItemNotFoundException() {
        givenCartIsMissing();

        assertThatThrownBy(() -> cartService.removeItem(session, NAIL_GUN_ITEM_ID))
                .isInstanceOf(CartItemNotFoundException.class);
    }

    @Test
    void requireNonEmptyCart_WhenCartHasPositions_ReturnsCart() {
        Cart cart = TestFixtures.cart(CART_ID, session);
        TestFixtures.cartItem(NAIL_GUN_ITEM_ID, cart, nailGun, TWO_ITEMS);
        givenCartExists(cart);

        assertThat(cartService.requireNonEmptyCart(session)).isEqualTo(cart);
    }

    @Test
    void requireNonEmptyCart_WhenCartHasNoPositions_ThrowsEmptyCartException() {
        givenCartExists(TestFixtures.cart(CART_ID, session));

        assertThatThrownBy(() -> cartService.requireNonEmptyCart(session))
                .isInstanceOf(EmptyCartException.class);
    }

    @Test
    void requireNonEmptyCart_WhenSessionHasNoCart_ThrowsEmptyCartException() {
        givenCartIsMissing();

        assertThatThrownBy(() -> cartService.requireNonEmptyCart(session))
                .isInstanceOf(EmptyCartException.class);
    }

    private void givenCartExists(Cart cart) {
        given(cartRepository.findBySession(session)).willReturn(Optional.of(cart));
    }

    private void givenCartIsMissing() {
        given(cartRepository.findBySession(session)).willReturn(Optional.empty());
    }

    private void givenCartIsSaved() {
        given(cartRepository.save(any(Cart.class))).willAnswer(invocation -> invocation.getArgument(0));
    }
}
