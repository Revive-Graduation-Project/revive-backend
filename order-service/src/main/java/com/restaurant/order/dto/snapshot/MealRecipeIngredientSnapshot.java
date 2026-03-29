package com.restaurant.order.dto.snapshot;

import java.util.UUID;

public record MealRecipeIngredientSnapshot(
        UUID ingredientId,
        Double quantityGrams
) {}
