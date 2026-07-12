package com.restaurant.order.dto.request;

import java.util.Map;

public record ReserveMealRequest(
        Long mealId,
        int quantity,
        Map<Long, Double> customizations
) {}
