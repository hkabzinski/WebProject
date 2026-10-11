package com.example.store.order;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.example.store.catalog.Product;
import com.example.store.support.TestFixtures;
import com.example.store.user.User;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DefaultOrderServiceTest {

    private static final long USER_ID = 1L;
    private static final long NAIL_GUN_ID = 2411L;
    private static final long ORDER_ID = 900L;
    private static final long OTHER_ORDER_ID = 901L;
    private static final long ORDER_ITEM_ID = 500L;
    private static final String NAIL_GUN_TITLE = "Nail gun";
    private static final String NAIL_GUN_PRICE = "23.95";
    private static final int NAIL_GUN_STOCK = 6;
    private static final int TWO_ITEMS = 2;
    private static final String EXPECTED_ORDER_TOTAL = "47.90";
    private static final String EMAIL = "my@email.com";
    private static final String PASSWORD_HASH = "$2a$10$encoded";
    private static final Instant ORDER_DATE = Instant.parse("2026-01-01T10:00:00Z");

    @Mock
    private OrderRepository orderRepository;

    @InjectMocks
    private DefaultOrderService orderService;

    private User user;
    private Product nailGun;

    @BeforeEach
    void createUserAndProduct() {
        user = TestFixtures.user(USER_ID, EMAIL, PASSWORD_HASH);
        nailGun = TestFixtures.product(NAIL_GUN_ID, NAIL_GUN_TITLE, NAIL_GUN_STOCK, NAIL_GUN_PRICE);
    }

    @Test
    void findOrderHistory_WhenUserHasOrders_ReturnsIdentifierDateTotalAndStatus() {
        given(orderRepository.findByUserOrderByCreatedAtDesc(user)).willReturn(List.of(orderWithNailGuns()));

        List<OrderResponse> history = orderService.findOrderHistory(user);

        assertThat(history).singleElement()
                .returns(String.valueOf(ORDER_ID), OrderResponse::id)
                .returns(ORDER_DATE, OrderResponse::createdAt)
                .returns(EXPECTED_ORDER_TOTAL, OrderResponse::total)
                .returns(OrderStatus.CONFIRMED, OrderResponse::status);
    }

    @Test
    void findOrderHistory_WhenUserHasNoOrders_ReturnsEmptyList() {
        given(orderRepository.findByUserOrderByCreatedAtDesc(user)).willReturn(List.of());

        assertThat(orderService.findOrderHistory(user)).isEmpty();
    }

    @Test
    void cancelOrder_WhenOrderIsConfirmed_CancelsItAndReturnsProductsToStock() {
        Order order = orderWithNailGuns();
        given(orderRepository.findByIdAndUser(ORDER_ID, user)).willReturn(Optional.of(order));
        given(orderRepository.save(order)).willReturn(order);

        OrderResponse response = orderService.cancelOrder(user, ORDER_ID);

        assertThat(response.status()).isEqualTo(OrderStatus.CANCELLED);
        assertThat(nailGun.getAvailable()).isEqualTo(NAIL_GUN_STOCK + TWO_ITEMS);
    }

    @Test
    void cancelOrder_WhenOrderIsAlreadyCancelled_ThrowsOrderNotCancellableException() {
        Order order = orderWithNailGuns();
        order.cancel();
        given(orderRepository.findByIdAndUser(ORDER_ID, user)).willReturn(Optional.of(order));

        assertThatThrownBy(() -> orderService.cancelOrder(user, ORDER_ID))
                .isInstanceOf(OrderNotCancellableException.class);

        assertThat(nailGun.getAvailable()).isEqualTo(NAIL_GUN_STOCK + TWO_ITEMS);
        verify(orderRepository, never()).save(any(Order.class));
    }

    @Test
    void cancelOrder_WhenOrderBelongsToAnotherUser_ThrowsOrderNotFoundException() {
        given(orderRepository.findByIdAndUser(OTHER_ORDER_ID, user)).willReturn(Optional.empty());

        assertThatThrownBy(() -> orderService.cancelOrder(user, OTHER_ORDER_ID))
                .isInstanceOf(OrderNotFoundException.class);

        assertThat(nailGun.getAvailable()).isEqualTo(NAIL_GUN_STOCK);
    }

    private Order orderWithNailGuns() {
        Order order = TestFixtures.order(ORDER_ID, user, ORDER_DATE);
        TestFixtures.orderItem(ORDER_ITEM_ID, order, nailGun, TWO_ITEMS);
        return order;
    }
}
