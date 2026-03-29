package com.restaurant.kitchen.dto;

import java.time.Instant;

public record ErrorResponse(
        String message,
        Instant timestamp
) {}
