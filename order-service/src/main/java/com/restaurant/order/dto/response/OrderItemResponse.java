package com.restaurant.order.dto.response;

import java.math.BigDecimal;
import java.util.Map;

public record OrderItemResponse(
                Long id,
                Long mealId,
                String snapshotName,
                BigDecimal snapshotPrice,
                String snapshotImageUrl,
                Integer quantity,
                Map<String, Object> customizations) {
}
