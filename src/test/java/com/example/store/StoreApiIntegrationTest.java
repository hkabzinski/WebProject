package com.example.store;

import static com.example.store.session.AuthenticatedSessionArgumentResolver.SESSION_HEADER;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.store.order.OrderStatus;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class StoreApiIntegrationTest {

    private static final String REGISTER_PATH = "/api/users/register";
    private static final String LOGIN_PATH = "/api/users/login";
    private static final String PASSWORD_RESET_REQUEST_PATH = "/api/users/password-reset/request";
    private static final String PASSWORD_RESET_PATH = "/api/users/password-reset";
    private static final String PRODUCTS_PATH = "/api/products";
    private static final String CART_PATH = "/api/cart";
    private static final String CART_ITEM_PATH = "/api/cart/items/{itemId}";
    private static final String CHECKOUT_PATH = "/api/checkout";
    private static final String ORDERS_PATH = "/api/orders";
    private static final String CANCEL_ORDER_PATH = "/api/orders/{orderId}/cancel";

    private static final String CREDENTIALS_TEMPLATE = """
            {"email":"%s","password":"%s"}""";
    private static final String ADD_POSITION_TEMPLATE = """
            {"id":"%d","quantity":"%d"}""";
    private static final String CHANGE_QUANTITY_TEMPLATE = """
            {"quantity":%d}""";
    private static final String PASSWORD_RESET_REQUEST_TEMPLATE = """
            {"email":"%s"}""";
    private static final String PASSWORD_RESET_TEMPLATE = """
            {"token":"%s","newPassword":"%s"}""";

    private static final String PASSWORD = "123";
    private static final String NEW_PASSWORD = "456";
    private static final long NAIL_GUN_ID = 2411L;
    private static final String NAIL_GUN_TITLE = "Nail gun";
    private static final int NAIL_GUN_STOCK = 8;
    private static final int TWO_ITEMS = 2;
    private static final int THREE_ITEMS = 3;
    private static final int EXCESSIVE_QUANTITY = NAIL_GUN_STOCK + 1;
    private static final int FIRST_ORDINAL = 1;
    private static final String TWO_NAIL_GUNS_TOTAL = "47.90";
    private static final String THREE_NAIL_GUNS_TOTAL = "71.85";
    private static final String SESSION_ID_PATH = "$.sessionId";
    private static final String FIRST_ITEM_ID_PATH = "$.items[0].itemId";
    private static final String RESET_TOKEN_PATH = "$.resetToken";
    private static final String ORDER_ID_PATH = "$.id";
    private static final String UNKNOWN_SESSION_ID = "00000000-0000-0000-0000-000000000000";
    private static final String CONFIRMED_STATUS = OrderStatus.CONFIRMED.name();
    private static final String CANCELLED_STATUS = OrderStatus.CANCELLED.name();

    @Autowired
    private MockMvc mockMvc;

    @Test
    void customerJourney_FromRegistrationToCheckout_DeductsStockAndRecordsOrder() throws Exception {
        String email = "journey@email.com";
        registerUser(email);
        String sessionId = login(email, PASSWORD);

        mockMvc.perform(get(PRODUCTS_PATH))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.id == '%d')].available".formatted(NAIL_GUN_ID)).value(NAIL_GUN_STOCK));

        mockMvc.perform(post(CART_PATH)
                        .header(SESSION_HEADER, sessionId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(ADD_POSITION_TEMPLATE.formatted(NAIL_GUN_ID, TWO_ITEMS)))
                .andExpect(status().isCreated());

        mockMvc.perform(get(CART_PATH).header(SESSION_HEADER, sessionId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].ordinal").value(FIRST_ORDINAL))
                .andExpect(jsonPath("$.items[0].title").value(NAIL_GUN_TITLE))
                .andExpect(jsonPath("$.items[0].quantity").value(TWO_ITEMS))
                .andExpect(jsonPath("$.total").value(TWO_NAIL_GUNS_TOTAL));

        mockMvc.perform(post(CHECKOUT_PATH).header(SESSION_HEADER, sessionId))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.total").value(TWO_NAIL_GUNS_TOTAL));

        mockMvc.perform(get(PRODUCTS_PATH))
                .andExpect(jsonPath("$[?(@.id == '%d')].available".formatted(NAIL_GUN_ID))
                        .value(NAIL_GUN_STOCK - TWO_ITEMS));

        mockMvc.perform(get(CART_PATH).header(SESSION_HEADER, sessionId))
                .andExpect(jsonPath("$.items").isEmpty());

        mockMvc.perform(get(ORDERS_PATH).header(SESSION_HEADER, sessionId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].total").value(TWO_NAIL_GUNS_TOTAL))
                .andExpect(jsonPath("$[0].status").value(CONFIRMED_STATUS));
    }

    @Test
    void viewCart_WhenNewSessionIsOpened_StartsEmptyForThatSession() throws Exception {
        String email = "sessions@email.com";
        registerUser(email);
        String firstSessionId = login(email, PASSWORD);
        addNailGuns(firstSessionId, TWO_ITEMS);

        String secondSessionId = login(email, PASSWORD);

        mockMvc.perform(get(CART_PATH).header(SESSION_HEADER, secondSessionId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items").isEmpty());

        mockMvc.perform(get(CART_PATH).header(SESSION_HEADER, firstSessionId))
                .andExpect(jsonPath("$.items[0].quantity").value(TWO_ITEMS));
    }

    @Test
    void viewCart_WhenSessionIsUnknown_ReturnsUnauthorized() throws Exception {
        mockMvc.perform(get(CART_PATH).header(SESSION_HEADER, UNKNOWN_SESSION_ID))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void registerUser_WhenEmailIsAlreadyRegistered_ReturnsConflict() throws Exception {
        String email = "duplicate@email.com";
        registerUser(email);

        mockMvc.perform(post(REGISTER_PATH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(CREDENTIALS_TEMPLATE.formatted(email, PASSWORD)))
                .andExpect(status().isConflict());
    }

    @Test
    void addPosition_WhenQuantityExceedsStock_ReturnsConflict() throws Exception {
        String email = "stock@email.com";
        registerUser(email);
        String sessionId = login(email, PASSWORD);

        mockMvc.perform(post(CART_PATH)
                        .header(SESSION_HEADER, sessionId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(ADD_POSITION_TEMPLATE.formatted(NAIL_GUN_ID, EXCESSIVE_QUANTITY)))
                .andExpect(status().isConflict());
    }

    @Test
    void changeQuantityAndRemoveItem_UpdateCartContent() throws Exception {
        String email = "modify@email.com";
        registerUser(email);
        String sessionId = login(email, PASSWORD);
        String itemId = addNailGuns(sessionId, TWO_ITEMS);

        mockMvc.perform(patch(CART_ITEM_PATH, itemId)
                        .header(SESSION_HEADER, sessionId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(CHANGE_QUANTITY_TEMPLATE.formatted(THREE_ITEMS)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(THREE_NAIL_GUNS_TOTAL));

        mockMvc.perform(delete(CART_ITEM_PATH, itemId).header(SESSION_HEADER, sessionId))
                .andExpect(status().isNoContent());

        mockMvc.perform(get(CART_PATH).header(SESSION_HEADER, sessionId))
                .andExpect(jsonPath("$.items").isEmpty());
    }

    @Test
    void cancelOrder_AfterCheckout_ReturnsProductsToStock() throws Exception {
        String email = "cancel@email.com";
        registerUser(email);
        String sessionId = login(email, PASSWORD);
        addNailGuns(sessionId, TWO_ITEMS);

        String checkoutResponse = mockMvc.perform(post(CHECKOUT_PATH).header(SESSION_HEADER, sessionId))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        String orderId = JsonPath.read(checkoutResponse, ORDER_ID_PATH);

        mockMvc.perform(post(CANCEL_ORDER_PATH, orderId).header(SESSION_HEADER, sessionId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(CANCELLED_STATUS));

        mockMvc.perform(get(PRODUCTS_PATH))
                .andExpect(jsonPath("$[?(@.id == '%d')].available".formatted(NAIL_GUN_ID)).value(NAIL_GUN_STOCK));

        mockMvc.perform(post(CANCEL_ORDER_PATH, orderId).header(SESSION_HEADER, sessionId))
                .andExpect(status().isConflict());
    }

    @Test
    void resetPassword_WhenTokenIsValid_AllowsLoginWithNewPassword() throws Exception {
        String email = "reset@email.com";
        registerUser(email);

        String tokenResponse = mockMvc.perform(post(PASSWORD_RESET_REQUEST_PATH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(PASSWORD_RESET_REQUEST_TEMPLATE.formatted(email)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        String resetToken = JsonPath.read(tokenResponse, RESET_TOKEN_PATH);

        mockMvc.perform(post(PASSWORD_RESET_PATH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(PASSWORD_RESET_TEMPLATE.formatted(resetToken, NEW_PASSWORD)))
                .andExpect(status().isNoContent());

        mockMvc.perform(post(LOGIN_PATH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(CREDENTIALS_TEMPLATE.formatted(email, PASSWORD)))
                .andExpect(status().isUnauthorized());

        assertThat(login(email, NEW_PASSWORD)).isNotBlank();
    }

    private void registerUser(String email) throws Exception {
        mockMvc.perform(post(REGISTER_PATH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(CREDENTIALS_TEMPLATE.formatted(email, PASSWORD)))
                .andExpect(status().isOk());
    }

    private String login(String email, String password) throws Exception {
        String response = mockMvc.perform(post(LOGIN_PATH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(CREDENTIALS_TEMPLATE.formatted(email, password)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(response, SESSION_ID_PATH);
    }

    private String addNailGuns(String sessionId, int quantity) throws Exception {
        String response = mockMvc.perform(post(CART_PATH)
                        .header(SESSION_HEADER, sessionId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(ADD_POSITION_TEMPLATE.formatted(NAIL_GUN_ID, quantity)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(response, FIRST_ITEM_ID_PATH);
    }
}
