package com.example.store.order;

import static com.example.store.session.AuthenticatedSessionArgumentResolver.SESSION_HEADER;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.store.cart.EmptyCartException;
import com.example.store.catalog.InsufficientStockException;
import com.example.store.session.SessionService;
import com.example.store.session.UserSession;
import com.example.store.support.TestFixtures;
import com.example.store.user.User;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest({CheckoutController.class, OrderController.class})
class OrderEndpointsTest {

    private static final String CHECKOUT_PATH = "/api/checkout";
    private static final String ORDERS_PATH = "/api/orders";
    private static final String CANCEL_ORDER_PATH = "/api/orders/{orderId}/cancel";
    private static final long USER_ID = 1L;
    private static final long ORDER_ID = 900L;
    private static final long NAIL_GUN_ID = 2411L;
    private static final String NAIL_GUN_TITLE = "Nail gun";
    private static final String NAIL_GUN_PRICE = "23.95";
    private static final int TWO_ITEMS = 2;
    private static final int AVAILABLE_STOCK = 1;
    private static final int FIRST_ORDINAL = 1;
    private static final String ORDER_TOTAL = "47.90";
    private static final String SESSION_ID = "aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee";
    private static final String EMAIL = "my@email.com";
    private static final String PASSWORD_HASH = "$2a$10$encoded";
    private static final Instant NOW = Instant.parse("2026-01-01T10:00:00Z");
    private static final Instant SESSION_EXPIRY = Instant.parse("2026-01-01T10:30:00Z");

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CheckoutService checkoutService;

    @MockitoBean
    private OrderService orderService;

    @MockitoBean
    private SessionService sessionService;

    private User user;
    private UserSession session;

    @BeforeEach
    void authenticateSession() {
        user = TestFixtures.user(USER_ID, EMAIL, PASSWORD_HASH);
        session = TestFixtures.session(SESSION_ID, user, NOW, SESSION_EXPIRY);
        given(sessionService.requireActiveSession(SESSION_ID)).willReturn(session);
    }

    @Test
    void checkout_WhenCartCanBePurchased_ReturnsCreatedOrder() throws Exception {
        given(checkoutService.checkout(session)).willReturn(orderWithStatus(OrderStatus.CONFIRMED));

        mockMvc.perform(post(CHECKOUT_PATH).header(SESSION_HEADER, SESSION_ID))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(String.valueOf(ORDER_ID)))
                .andExpect(jsonPath("$.status").value(OrderStatus.CONFIRMED.name()))
                .andExpect(jsonPath("$.total").value(ORDER_TOTAL))
                .andExpect(jsonPath("$.items[0].ordinal").value(FIRST_ORDINAL));
    }

    @Test
    void checkout_WhenCartIsEmpty_ReturnsBadRequest() throws Exception {
        willThrow(new EmptyCartException()).given(checkoutService).checkout(any(UserSession.class));

        mockMvc.perform(post(CHECKOUT_PATH).header(SESSION_HEADER, SESSION_ID))
                .andExpect(status().isBadRequest());
    }

    @Test
    void checkout_WhenStockBecameInsufficient_ReturnsConflict() throws Exception {
        willThrow(new InsufficientStockException(NAIL_GUN_TITLE, AVAILABLE_STOCK, TWO_ITEMS))
                .given(checkoutService).checkout(any(UserSession.class));

        mockMvc.perform(post(CHECKOUT_PATH).header(SESSION_HEADER, SESSION_ID))
                .andExpect(status().isConflict());
    }

    @Test
    void checkout_WhenSessionHeaderIsMissing_ReturnsUnauthorized() throws Exception {
        mockMvc.perform(post(CHECKOUT_PATH))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void listOrders_WhenUserHasOrders_ReturnsIdentifierDateTotalAndStatus() throws Exception {
        given(orderService.findOrderHistory(user)).willReturn(List.of(orderWithStatus(OrderStatus.CONFIRMED)));

        mockMvc.perform(get(ORDERS_PATH).header(SESSION_HEADER, SESSION_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(String.valueOf(ORDER_ID)))
                .andExpect(jsonPath("$[0].createdAt").exists())
                .andExpect(jsonPath("$[0].total").value(ORDER_TOTAL))
                .andExpect(jsonPath("$[0].status").value(OrderStatus.CONFIRMED.name()));
    }

    @Test
    void cancelOrder_WhenOrderIsConfirmed_ReturnsCancelledOrder() throws Exception {
        given(orderService.cancelOrder(user, ORDER_ID)).willReturn(orderWithStatus(OrderStatus.CANCELLED));

        mockMvc.perform(post(CANCEL_ORDER_PATH, ORDER_ID).header(SESSION_HEADER, SESSION_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(OrderStatus.CANCELLED.name()));
    }

    @Test
    void cancelOrder_WhenOrderIsAlreadyCancelled_ReturnsConflict() throws Exception {
        willThrow(new OrderNotCancellableException(ORDER_ID, OrderStatus.CANCELLED))
                .given(orderService).cancelOrder(any(User.class), anyLong());

        mockMvc.perform(post(CANCEL_ORDER_PATH, ORDER_ID).header(SESSION_HEADER, SESSION_ID))
                .andExpect(status().isConflict());
    }

    @Test
    void cancelOrder_WhenOrderDoesNotExist_ReturnsNotFound() throws Exception {
        willThrow(new OrderNotFoundException(ORDER_ID)).given(orderService).cancelOrder(any(User.class), anyLong());

        mockMvc.perform(post(CANCEL_ORDER_PATH, ORDER_ID).header(SESSION_HEADER, SESSION_ID))
                .andExpect(status().isNotFound());
    }

    private static OrderResponse orderWithStatus(OrderStatus status) {
        OrderItemResponse position = new OrderItemResponse(
                FIRST_ORDINAL,
                String.valueOf(NAIL_GUN_ID),
                NAIL_GUN_TITLE,
                TWO_ITEMS,
                NAIL_GUN_PRICE,
                ORDER_TOTAL);
        return new OrderResponse(String.valueOf(ORDER_ID), NOW, ORDER_TOTAL, status, List.of(position));
    }
}
