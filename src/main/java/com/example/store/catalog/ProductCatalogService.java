package com.example.store.catalog;

import java.util.List;

public interface ProductCatalogService {

    List<ProductResponse> listAvailableProducts();

    Product requireProduct(Long productId);
}
