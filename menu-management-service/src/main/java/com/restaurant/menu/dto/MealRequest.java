package com.restaurant.menu.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.util.List;

public record MealRequest(
        @NotBlank(message = "Meal name is required")
        String name,
        
        @NotNull(message = "Price is required")
        @Positive(message = "Price must be greater than 0")
        Double price,
        
        @NotBlank(message = "Category is required")
        String category,
        
        @NotEmpty(message = "Meal must have at least one ingredient")
        List<IngredientQuantity> ingredients
) {
    public record IngredientQuantity(
            @NotNull(message = "Ingredient ID is required")
            Long ingredientId,
            
            @NotNull(message = "Quantity is required")
            @Positive(message = "Quantity must be greater than 0")
            Double quantity // in grams
    ) {}
}
