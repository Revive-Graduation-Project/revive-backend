package com.restaurant.auth.dto;

public record AuthResponse(String token, String role, Long userId, String emailString) {
}
