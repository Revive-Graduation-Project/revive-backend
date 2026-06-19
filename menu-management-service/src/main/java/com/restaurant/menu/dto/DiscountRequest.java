package com.restaurant.menu.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record DiscountRequest(
        @NotNull(message = "hasDiscount is required")
        Boolean hasDiscount,

        @Min(value = 0, message = "Discount percentage must be at least 0")
        @Max(value = 100, message = "Discount percentage cannot exceed 100")
        Double discountPercentage
) {}
