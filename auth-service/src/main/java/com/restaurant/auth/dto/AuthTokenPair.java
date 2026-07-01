package com.restaurant.auth.dto;

public record AuthTokenPair(String token, String refreshToken, String role, Long userId, String emailString, String firstName, String lastName) {
}
