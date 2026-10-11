package com.example.store.order;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.never;
import static org.assertj.core.groups.Tuple.tuple;
import static org.mockito.Mockito.verify;

import com.example.store.cart.Cart;
import com.example.store.cart.CartService;
import com.example.store.cart.EmptyCartException;
import com.example.store.catalog.InsufficientStockException;
import com.example.store.catalog.Product;
import com.example.store.session.UserSession;
import com.example.store.support.TestFixtures;
import com.example.store.user.User;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DefaultCheckoutServiceTest {

    private static final long USER_ID = 1L;
    private static final long CART_ID = 70L;
    private static final long ORDER_ID = 900L;
    private static final long NAIL_GUN_ID = 2411L;
    private static final long HAMMER_ID = 364L;
    private static final long NAIL_GUN_ITEM_ID = 100L;
    private static final long HAMMER_ITEM_ID = 101L;
    private static final String NAIL_GUN_TITLE = "Nail gun";
    private static final String HAMMER_TITLE = "Claw hammer";
    private static final String NAIL_GUN_PRICE = "23.95";
    private static final String HAMMER_PRICE = "12.75";
    private static final String STALE_NAIL_GUN_PRICE = "19.95";
    private static final int NAIL_GUN_STOCK = 8;
    private static final int HAMMER_STOCK = 42;
    private static final int TWO_ITEMS = 2;
    private static final int THREE_ITEMS = 3;
    private static final int MORE_THAN_STOCK = NAIL_GUN_STOCK + 1;
    private static final String EXPECTED_ORDER_TOTAL = "86.15";
    private static final String SESSION_ID = "aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee";
    private static final String EMAIL = "my@email.com";
    private static final String PASSWORD_HASH = "$2a$10$encoded";
    private static final Instant NOW = Instant.parse("2026-01-01T10:00:00Z");

    @Mock
    private CartService cartService;

    @Mock
    private OrderRepository orderRepository;

    private DefaultCheckoutService checkoutService;

    private UserSession session;
    private Product nailGun;
    private Product hammer;
    private Cart cart;

    @BeforeEach
    void createServiceAndCart() {
        checkoutService = new DefaultCheckoutService(cartService, orderRepository, Clock.fixed(NOW, ZoneOffset.UTC));
        User user = TestFixtures.user(USER_ID, EMAIL, PASSWORD_HASH);
        session = TestFixtures.session(SESSION_ID, user, NOW, NOW.plusSeconds(1800));
        nailGun = TestFixtures.product(NAIL_GUN_ID, NAIL_GUN_TITLE, NAIL_GUN_STOCK, NAIL_GUN_PRICE);
        hammer = TestFixtures.product(HAMMER_ID, HAMMER_TITLE, HAMMER_STOCK, HAMMER_PRICE);
        cart = TestFixtures.cart(CART_ID, session);
    }

    @Test
    void checkout_WhenCartIsEmpty_ThrowsEmptyCartException() {
        willThrow(new EmptyCartException()).given(cartService).requireNonEmptyCart(session);

        assertThatThrownBy(() -> checkoutService.checkout(session))
                .isInstanceOf(EmptyCartException.class);

        verify(orderRepository, never()).save(any(Order.class));
    }

    @Test
    void checkout_WhenStockIsAvailable_DeductsStockAndClearsCart() {
        TestFixtures.cartItem(NAIL_GUN_ITEM_ID, cart, nailGun, TWO_ITEMS);
        TestFixtures.cartItem(HAMMER_ITEM_ID, cart, hammer, THREE_ITEMS);
        givenCartIsReadyForCheckout();
        givenOrderIsSaved();

        checkoutService.checkout(session);

        assertThat(nailGun.getAvailable()).isEqualTo(NAIL_GUN_STOCK - TWO_ITEMS);
        assertThat(hammer.getAvailable()).isEqualTo(HAMMER_STOCK - THREE_ITEMS);
        assertThat(cart.isEmpty()).isTrue();
    }

    @Test
    void checkout_WhenStockIsAvailable_ReturnsConfirmedOrderWithCartPositions() {
        TestFixtures.cartItem(NAIL_GUN_ITEM_ID, cart, nailGun, TWO_ITEMS);
        TestFixtures.cartItem(HAMMER_ITEM_ID, cart, hammer, THREE_ITEMS);
        givenCartIsReadyForCheckout();
        givenOrderIsSaved();

        OrderResponse response = checkoutService.checkout(session);

        assertThat(response.id()).isEqualTo(String.valueOf(ORDER_ID));
        assertThat(response.status()).isEqualTo(OrderStatus.CONFIRMED);
        assertThat(response.createdAt()).isEqualTo(NOW);
        assertThat(response.total()).isEqualTo(EXPECTED_ORDER_TOTAL);
        assertThat(response.items()).extracting(OrderItemResponse::title, OrderItemResponse::quantity)
                .containsExactly(
                        tuple(NAIL_GUN_TITLE, TWO_ITEMS),
                        tuple(HAMMER_TITLE, THREE_ITEMS));
    }

    @Test
    void checkout_WhenStockBecameInsufficient_ThrowsExceptionAndLeavesStockUntouched() {
        TestFixtures.cartItem(NAIL_GUN_ITEM_ID, cart, nailGun, MORE_THAN_STOCK);
        TestFixtures.cartItem(HAMMER_ITEM_ID, cart, hammer, THREE_ITEMS);
        givenCartIsReadyForCheckout();

        assertThatThrownBy(() -> checkoutService.checkout(session))
                .isInstanceOf(InsufficientStockException.class);

        assertThat(nailGun.getAvailable()).isEqualTo(NAIL_GUN_STOCK);
        assertThat(hammer.getAvailable()).isEqualTo(HAMMER_STOCK);
        assertThat(cart.isEmpty()).isFalse();
        verify(orderRepository, never()).save(any(Order.class));
    }

    @Test
    void checkout_WhenCartPriceNoLongerMatchesCatalogPrice_ThrowsPriceChangedException() {
        TestFixtures.cartItem(NAIL_GUN_ITEM_ID, cart, nailGun, TWO_ITEMS, new BigDecimal(STALE_NAIL_GUN_PRICE));
        givenCartIsReadyForCheckout();

        assertThatThrownBy(() -> checkoutService.checkout(session))
                .isInstanceOf(PriceChangedException.class);

        assertThat(nailGun.getAvailable()).isEqualTo(NAIL_GUN_STOCK);
        verify(orderRepository, never()).save(any(Order.class));
    }

    private void givenCartIsReadyForCheckout() {
        given(cartService.requireNonEmptyCart(session)).willReturn(cart);
    }

    private void givenOrderIsSaved() {
        given(orderRepository.save(any(Order.class)))
                .willAnswer(invocation -> TestFixtures.withId(invocation.getArgument(0), ORDER_ID));
    }
}
