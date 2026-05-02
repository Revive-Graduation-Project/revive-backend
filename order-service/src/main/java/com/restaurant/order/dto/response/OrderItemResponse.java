package com.restaurant.order.dto.response;

import java.math.BigDecimal;
import java.util.UUID;

public record OrderItemResponse(
        Long id,
        Long mealId,
        String snapshotName,
        BigDecimal snapshotPrice,
        Integer quantity
) {}
