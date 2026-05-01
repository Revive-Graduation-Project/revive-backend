package com.restaurant.menu.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record ReserveMealDTO(
        @NotNull(message = "Meal ID is required")
        Long mealId,

        @Min(value = 1, message = "Quantity must be at least 1")
        int quantity) {
}
