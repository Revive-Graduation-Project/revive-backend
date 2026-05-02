package com.restaurant.order.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record OrderItemRequest(
        @NotNull(message = "Meal ID is required")
        Long mealId,

        @Min(value = 1, message = "Quantity must be at least 1")
        int quantity
) {}
