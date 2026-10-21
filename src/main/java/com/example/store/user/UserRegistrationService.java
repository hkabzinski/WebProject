package com.example.store.user;

public interface UserRegistrationService {

    UserResponse registerUser(String email, String rawPassword);
}
