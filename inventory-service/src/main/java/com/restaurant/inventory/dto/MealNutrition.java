package com.restaurant.inventory.dto;

import java.util.List;

public record MealNutrition(String mealName, List<NutrientInfo> nutrients) {
}