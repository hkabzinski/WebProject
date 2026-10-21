package com.example.store.catalog;

import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class DefaultProductCatalogService implements ProductCatalogService {

    private final ProductRepository productRepository;

    public DefaultProductCatalogService(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    @Override
    public List<ProductResponse> listAvailableProducts() {
        return productRepository.findAllByOrderByTitleAsc().stream()
                .map(ProductResponse::of)
                .toList();
    }

    @Override
    public Product requireProduct(Long productId) {
        return productRepository.findById(productId)
                .orElseThrow(() -> new ProductNotFoundException(productId));
    }
}
