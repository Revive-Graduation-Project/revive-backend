package com.restaurant.menu.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PositiveOrZero;

import java.util.List;
import java.util.Map;

public record IngredientDTO(
        Long id,

        @NotBlank(message = "Ingredient name is required")
        String name,

        String description,

        List<Map<String, Object>> nutrients,

        @PositiveOrZero(message = "Stock cannot be negative")
        Double stock) { // in grams
}
