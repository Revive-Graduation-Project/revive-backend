package com.restaurant.menu.dto;

import java.util.List;

public record IngredientNutrition(
                String ingredientName,
                double quantity,
                String unit,
                int fdcId,
                String description,
                String foodCategory,
                List<NutrientInfo> nutrients) {
}
