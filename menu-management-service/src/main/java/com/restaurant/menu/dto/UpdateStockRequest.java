package com.restaurant.menu.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

public record UpdateStockRequest(
        @NotNull(message = "Stock quantity is required")
        @PositiveOrZero(message = "Stock cannot be negative")
        Integer stock) {
}
