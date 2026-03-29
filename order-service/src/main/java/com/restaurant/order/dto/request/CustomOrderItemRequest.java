package com.restaurant.order.dto.request;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;

/**
 * Simplified request for custom items: the user only specifies the ingredient ID.
 * The portion size (grams) is determined by the system based on the ingredient's category.
 */
public record CustomOrderItemRequest(
        @NotNull(message = "Ingredient ID is required")
        UUID ingredientId
) {}
