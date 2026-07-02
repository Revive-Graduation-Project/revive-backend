package com.restaurant.order.dto.snapshot;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Minimal snapshot of meal data fetched from menu-service at order time.
 * Only price and name are stored — nutritional details stay in menu-service.
 */
public record MealPriceSnapshot(
        Long id,
        String name,
        BigDecimal price
) {}
