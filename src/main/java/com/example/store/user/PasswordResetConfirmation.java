package com.example.store.user;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record PasswordResetConfirmation(
        @NotBlank String token,
        @NotBlank @Size(min = 3, max = 72) String newPassword) {
}
