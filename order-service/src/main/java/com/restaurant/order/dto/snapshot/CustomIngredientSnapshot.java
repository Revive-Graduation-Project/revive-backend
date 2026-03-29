package com.restaurant.order.dto.snapshot;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Represents the data we expect back from inventory-service
 * when fetching an ingredient by ID for snapshotting.
 */
public record CustomIngredientSnapshot(
        UUID id,
        String name,
        String category,
        Double portionGrams,
        BigDecimal pricePer100Gram,
        Double caloriesPer100Gram,
        Double proteinPer100Gram,
        Double carbsPer100Gram,
        Double fatsPer100Gram
) {}
