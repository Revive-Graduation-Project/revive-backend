package com.restaurant.order.dto.snapshot;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * Represents the data we expect back from menu-service
 * when fetching a meal by ID for snapshotting.
 */
public record MealSnapshot(
        UUID id,
        String name,
        BigDecimal price,
        Boolean isAvailable,
        Double totalCalories,
        Double totalProtein,
        Double totalCarbs,
        Double totalFats,
        Integer maxQuantity,
        List<MealRecipeIngredientSnapshot> ingredients
) {}
