package com.restaurant.order.dto.response;

import java.math.BigDecimal;

public record OrderItemResponse(
                Long id,
                Long mealId,
                String snapshotName,
                BigDecimal snapshotPrice,
                String snapshotImageUrl,
                Integer quantity) {
}
