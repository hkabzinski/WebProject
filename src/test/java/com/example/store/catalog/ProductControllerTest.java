package com.example.store.catalog;

import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.store.session.SessionService;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(ProductController.class)
class ProductControllerTest {

    private static final String PRODUCTS_PATH = "/api/products";
    private static final String NAIL_GUN_ID = "2411";
    private static final String NAIL_GUN_TITLE = "Nail gun";
    private static final String NAIL_GUN_PRICE = "23.95";
    private static final int NAIL_GUN_STOCK = 8;

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ProductCatalogService productCatalogService;

    @MockitoBean
    private SessionService sessionService;

    @Test
    void listProducts_ReturnsIdentifierTitleAvailabilityAndPrice() throws Exception {
        given(productCatalogService.listAvailableProducts()).willReturn(
                List.of(new ProductResponse(NAIL_GUN_ID, NAIL_GUN_TITLE, NAIL_GUN_STOCK, NAIL_GUN_PRICE)));

        mockMvc.perform(get(PRODUCTS_PATH))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(NAIL_GUN_ID))
                .andExpect(jsonPath("$[0].title").value(NAIL_GUN_TITLE))
                .andExpect(jsonPath("$[0].available").value(NAIL_GUN_STOCK))
                .andExpect(jsonPath("$[0].price").value(NAIL_GUN_PRICE));
    }

    @Test
    void listProducts_WhenCatalogIsEmpty_ReturnsEmptyList() throws Exception {
        given(productCatalogService.listAvailableProducts()).willReturn(List.of());

        mockMvc.perform(get(PRODUCTS_PATH))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isEmpty());
    }
}
