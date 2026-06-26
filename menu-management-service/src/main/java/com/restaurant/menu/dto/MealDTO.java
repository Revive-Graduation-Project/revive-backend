package com.restaurant.menu.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.util.List;
import java.util.Map;

public record MealDTO(
        Long id,

        @NotBlank(message = "Meal name is required")
        String name,

        String description,

        List<Map<String, Object>> nutrients,

        @NotNull(message = "Price is required")
        @Positive(message = "Price must be greater than 0")
        Double price,

        @NotBlank(message = "Category is required")
        String category,

        Boolean isActive,

        Boolean hasDiscount,

        Double discountPercentage,

        String imageUrl,

        @Valid
        List<MealIngredientDTO> mealIngredients) {
}
