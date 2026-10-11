package com.example.store.user;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CredentialsRequest(
        @NotBlank @Email String email,
        @NotBlank @Size(min = 3, max = 72) String password) {
}
