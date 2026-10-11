package com.example.store.cart;

import static com.example.store.session.AuthenticatedSessionArgumentResolver.SESSION_HEADER;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.store.catalog.InsufficientStockException;
import com.example.store.session.InvalidSessionException;
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
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(CartController.class)
class CartControllerTest {

    private static final String CART_PATH = "/api/cart";
    private static final String CART_ITEM_PATH = "/api/cart/items/{itemId}";
    private static final long USER_ID = 1L;
    private static final long NAIL_GUN_ID = 2411L;
    private static final long CART_ITEM_ID = 100L;
    private static final String NAIL_GUN_TITLE = "Nail gun";
    private static final String NAIL_GUN_PRICE = "23.95";
    private static final int TWO_ITEMS = 2;
    private static final int THREE_ITEMS = 3;
    private static final int INVALID_QUANTITY = 0;
    private static final int EXCESSIVE_QUANTITY = 99;
    private static final int AVAILABLE_STOCK = 8;
    private static final int FIRST_ORDINAL = 1;
    private static final String SUBTOTAL = "47.90";
    private static final String ADD_POSITION_TEMPLATE = """
            {"id":"%d","quantity":"%d"}""";
    private static final String CHANGE_QUANTITY_TEMPLATE = """
            {"quantity":%d}""";
    private static final String ADD_POSITION_BODY = ADD_POSITION_TEMPLATE.formatted(NAIL_GUN_ID, TWO_ITEMS);
    private static final String ADD_EXCESSIVE_POSITION_BODY =
            ADD_POSITION_TEMPLATE.formatted(NAIL_GUN_ID, EXCESSIVE_QUANTITY);
    private static final String INVALID_QUANTITY_BODY = ADD_POSITION_TEMPLATE.formatted(NAIL_GUN_ID, INVALID_QUANTITY);
    private static final String CHANGE_QUANTITY_BODY = CHANGE_QUANTITY_TEMPLATE.formatted(THREE_ITEMS);
    private static final String SESSION_ID = "aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee";
    private static final String EXPIRED_SESSION_ID = "bbbbbbbb-cccc-dddd-eeee-ffffffffffff";
    private static final String EMAIL = "my@email.com";
    private static final String PASSWORD_HASH = "$2a$10$encoded";
    private static final Instant NOW = Instant.parse("2026-01-01T10:00:00Z");
    private static final Instant SESSION_EXPIRY = Instant.parse("2026-01-01T10:30:00Z");

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CartService cartService;

    @MockitoBean
    private SessionService sessionService;

    private UserSession session;

    @BeforeEach
    void authenticateSession() {
        User user = TestFixtures.user(USER_ID, EMAIL, PASSWORD_HASH);
        session = TestFixtures.session(SESSION_ID, user, NOW, SESSION_EXPIRY);
        given(sessionService.requireActiveSession(SESSION_ID)).willReturn(session);
    }

    @Test
    void addPosition_WhenStockIsSufficient_ReturnsCreatedWithUpdatedCart() throws Exception {
        given(cartService.addPosition(session, NAIL_GUN_ID, TWO_ITEMS)).willReturn(cartWithOnePosition());

        mockMvc.perform(post(CART_PATH)
                        .header(SESSION_HEADER, SESSION_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(ADD_POSITION_BODY))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.items[0].ordinal").value(FIRST_ORDINAL))
                .andExpect(jsonPath("$.items[0].title").value(NAIL_GUN_TITLE))
                .andExpect(jsonPath("$.items[0].quantity").value(TWO_ITEMS))
                .andExpect(jsonPath("$.total").value(SUBTOTAL));
    }

    @Test
    void addPosition_WhenStockIsInsufficient_ReturnsConflict() throws Exception {
        willThrow(new InsufficientStockException(NAIL_GUN_TITLE, AVAILABLE_STOCK, EXCESSIVE_QUANTITY))
                .given(cartService).addPosition(any(UserSession.class), anyLong(), anyInt());

        mockMvc.perform(post(CART_PATH)
                        .header(SESSION_HEADER, SESSION_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(ADD_EXCESSIVE_POSITION_BODY))
                .andExpect(status().isConflict());
    }

    @Test
    void addPosition_WhenSessionHeaderIsMissing_ReturnsUnauthorized() throws Exception {
        mockMvc.perform(post(CART_PATH).contentType(MediaType.APPLICATION_JSON).content(ADD_POSITION_BODY))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void addPosition_WhenSessionHasExpired_ReturnsUnauthorized() throws Exception {
        willThrow(InvalidSessionException.notActive(EXPIRED_SESSION_ID))
                .given(sessionService).requireActiveSession(EXPIRED_SESSION_ID);

        mockMvc.perform(post(CART_PATH)
                        .header(SESSION_HEADER, EXPIRED_SESSION_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(ADD_POSITION_BODY))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void addPosition_WhenQuantityIsNotPositive_ReturnsBadRequest() throws Exception {
        mockMvc.perform(post(CART_PATH)
                        .header(SESSION_HEADER, SESSION_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(INVALID_QUANTITY_BODY))
                .andExpect(status().isBadRequest());
    }

    @Test
    void viewCart_WhenCartHasPositions_ReturnsOrdinalsQuantitiesAndSubtotals() throws Exception {
        given(cartService.viewCart(session)).willReturn(cartWithOnePosition());

        mockMvc.perform(get(CART_PATH).header(SESSION_HEADER, SESSION_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].ordinal").value(FIRST_ORDINAL))
                .andExpect(jsonPath("$.items[0].subtotal").value(SUBTOTAL))
                .andExpect(jsonPath("$.total").value(SUBTOTAL));
    }

    @Test
    void viewCart_WhenSessionHasNoCart_ReturnsEmptyCart() throws Exception {
        given(cartService.viewCart(session)).willReturn(CartResponse.empty());

        mockMvc.perform(get(CART_PATH).header(SESSION_HEADER, SESSION_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items").isEmpty());
    }

    @Test
    void changeQuantity_WhenQuantityIsValid_ReturnsUpdatedCart() throws Exception {
        given(cartService.changeQuantity(session, CART_ITEM_ID, THREE_ITEMS)).willReturn(cartWithOnePosition());

        mockMvc.perform(patch(CART_ITEM_PATH, CART_ITEM_ID)
                        .header(SESSION_HEADER, SESSION_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(CHANGE_QUANTITY_BODY))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(SUBTOTAL));
    }

    @Test
    void changeQuantity_WhenItemIsNotInCart_ReturnsNotFound() throws Exception {
        willThrow(new CartItemNotFoundException(CART_ITEM_ID))
                .given(cartService).changeQuantity(any(UserSession.class), anyLong(), anyInt());

        mockMvc.perform(patch(CART_ITEM_PATH, CART_ITEM_ID)
                        .header(SESSION_HEADER, SESSION_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(CHANGE_QUANTITY_BODY))
                .andExpect(status().isNotFound());
    }

    @Test
    void removeItem_WhenItemIsInCart_ReturnsNoContent() throws Exception {
        mockMvc.perform(delete(CART_ITEM_PATH, CART_ITEM_ID).header(SESSION_HEADER, SESSION_ID))
                .andExpect(status().isNoContent());
    }

    @Test
    void removeItem_WhenItemIsNotInCart_ReturnsNotFound() throws Exception {
        willThrow(new CartItemNotFoundException(CART_ITEM_ID))
                .given(cartService).removeItem(any(UserSession.class), anyLong());

        mockMvc.perform(delete(CART_ITEM_PATH, CART_ITEM_ID).header(SESSION_HEADER, SESSION_ID))
                .andExpect(status().isNotFound());
    }

    private static CartResponse cartWithOnePosition() {
        CartItemResponse position = new CartItemResponse(
                FIRST_ORDINAL,
                String.valueOf(CART_ITEM_ID),
                String.valueOf(NAIL_GUN_ID),
                NAIL_GUN_TITLE,
                TWO_ITEMS,
                NAIL_GUN_PRICE,
                SUBTOTAL);
        return new CartResponse(List.of(position), SUBTOTAL);
    }
}
