package com.restaurant.inventory.dto;

import java.util.List;

public record UsdaFoodDetail(int fdcId, String description, List<NutrientInfo> foodNutrients) {}