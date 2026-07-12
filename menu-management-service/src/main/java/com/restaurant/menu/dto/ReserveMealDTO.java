package com.restaurant.menu.dto;

import jakarta.validation.constraints.Min;
import java.util.Map;

public record ReserveMealDTO(
        Long mealId,

        @Min(value = 1, message = "Quantity must be at least 1")
        int quantity,
        
        Map<Long, Double> customizations) {
}
