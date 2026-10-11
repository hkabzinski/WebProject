package com.example.store.user;

public record UserResponse(String id, String email) {

    public static UserResponse of(User user) {
        return new UserResponse(String.valueOf(user.getId()), user.getEmail());
    }
}
