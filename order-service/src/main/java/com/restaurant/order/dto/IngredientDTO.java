package com.restaurant.order.dto;

public record IngredientDTO(
        Long id,
        String name,
        String category,
        Double stock,
        Double price
) {}
