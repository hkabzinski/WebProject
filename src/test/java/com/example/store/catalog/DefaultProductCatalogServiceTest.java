package com.example.store.catalog;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

import com.example.store.support.TestFixtures;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DefaultProductCatalogServiceTest {

    private static final long NAIL_GUN_ID = 2411L;
    private static final long UNKNOWN_PRODUCT_ID = 1L;
    private static final String NAIL_GUN_TITLE = "Nail gun";
    private static final String NAIL_GUN_PRICE = "23.95";
    private static final String UNFORMATTED_PRICE = "23.9";
    private static final String UNFORMATTED_PRICE_WITH_TWO_DECIMALS = "23.90";
    private static final int NAIL_GUN_STOCK = 8;

    @Mock
    private ProductRepository productRepository;

    @InjectMocks
    private DefaultProductCatalogService productCatalogService;

    @Test
    void listAvailableProducts_ReturnsIdentifierTitleAvailabilityAndPrice() {
        Product nailGun = TestFixtures.product(NAIL_GUN_ID, NAIL_GUN_TITLE, NAIL_GUN_STOCK, NAIL_GUN_PRICE);
        given(productRepository.findAllByOrderByTitleAsc()).willReturn(List.of(nailGun));

        List<ProductResponse> products = productCatalogService.listAvailableProducts();

        assertThat(products).containsExactly(
                new ProductResponse(String.valueOf(NAIL_GUN_ID), NAIL_GUN_TITLE, NAIL_GUN_STOCK, NAIL_GUN_PRICE));
    }

    @Test
    void listAvailableProducts_WhenPriceHasFewerDecimals_FormatsPriceWithTwoDecimals() {
        Product nailGun = TestFixtures.product(NAIL_GUN_ID, NAIL_GUN_TITLE, NAIL_GUN_STOCK, UNFORMATTED_PRICE);
        given(productRepository.findAllByOrderByTitleAsc()).willReturn(List.of(nailGun));

        List<ProductResponse> products = productCatalogService.listAvailableProducts();

        assertThat(products).singleElement()
                .returns(UNFORMATTED_PRICE_WITH_TWO_DECIMALS, ProductResponse::price);
    }

    @Test
    void requireProduct_WhenProductExists_ReturnsProduct() {
        Product nailGun = TestFixtures.product(NAIL_GUN_ID, NAIL_GUN_TITLE, NAIL_GUN_STOCK, NAIL_GUN_PRICE);
        given(productRepository.findById(NAIL_GUN_ID)).willReturn(Optional.of(nailGun));

        assertThat(productCatalogService.requireProduct(NAIL_GUN_ID)).isEqualTo(nailGun);
    }

    @Test
    void requireProduct_WhenProductIsUnknown_ThrowsProductNotFoundException() {
        given(productRepository.findById(UNKNOWN_PRODUCT_ID)).willReturn(Optional.empty());

        assertThatThrownBy(() -> productCatalogService.requireProduct(UNKNOWN_PRODUCT_ID))
                .isInstanceOf(ProductNotFoundException.class);
    }
}
