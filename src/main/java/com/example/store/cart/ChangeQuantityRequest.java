package com.example.store.cart;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record ChangeQuantityRequest(@NotNull @Min(1) Integer quantity) {
}
