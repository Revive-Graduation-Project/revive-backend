package com.restaurant.kitchen.dto;

public record ErrorResponse(
        String message,
        String timestamp
) {}
