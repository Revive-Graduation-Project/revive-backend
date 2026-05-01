package com.restaurant.menu.dto;

/**
 * Represents one ingredient line within a meal response,
 * including the quantity (in grams) used per serving.
 */
public record MealIngredientDTO(
        Long id,
        IngredientDTO ingredient,
        Double quantityGrams) {
}
