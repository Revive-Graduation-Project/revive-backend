package com.restaurant.order.dto;

import java.time.Instant;

public record ErrorResponse(
        String message,
        Instant timestamp
) {}
