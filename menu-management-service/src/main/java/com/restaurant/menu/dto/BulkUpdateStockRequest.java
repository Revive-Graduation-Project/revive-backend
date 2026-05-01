package com.restaurant.menu.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

import java.util.List;

public record BulkUpdateStockRequest(
        @NotEmpty(message = "Stock updates list cannot be empty")
        List<@Valid StockEntry> updates) {

    public record StockEntry(
            @NotNull(message = "Ingredient ID is required")
            Long ingredientId,

            @NotNull(message = "Stock quantity is required")
            @PositiveOrZero(message = "Stock cannot be negative")
            Double stock) { // in grams
    }
}
