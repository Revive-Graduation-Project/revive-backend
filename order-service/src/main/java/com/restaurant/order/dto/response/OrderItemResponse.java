package com.restaurant.order.dto.response;

import java.math.BigDecimal;
import java.util.UUID;

public record OrderItemResponse(
        Long id,
        UUID mealId,
        String snapshotName,
        BigDecimal snapshotPrice,
        Integer quantity,
        Double snapshotCalories,
        Double snapshotProtein,
        Double snapshotCarbs,
        Double snapshotFats
) {}
