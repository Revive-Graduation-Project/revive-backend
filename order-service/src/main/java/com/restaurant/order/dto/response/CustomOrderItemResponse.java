package com.restaurant.order.dto.response;

import java.math.BigDecimal;
import java.util.UUID;

public record CustomOrderItemResponse(
        Long id,
        UUID ingredientId,
        String snapshotName,
        Double quantityGrams,
        BigDecimal snapshotPricePer100Gram,
        Double snapshotCaloriesPer100Gram,
        Double snapshotProteinPer100Gram,
        Double snapshotCarbsPer100Gram,
        Double snapshotFatsPer100Gram
) {}
