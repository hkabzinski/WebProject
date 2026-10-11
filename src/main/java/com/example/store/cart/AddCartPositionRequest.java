package com.example.store.cart;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record AddCartPositionRequest(
        @NotNull Long id,
        @NotNull @Min(1) Integer quantity) {
}
