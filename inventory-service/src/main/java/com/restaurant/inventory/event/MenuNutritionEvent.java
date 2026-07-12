package com.restaurant.inventory.event;

import com.restaurant.inventory.dto.MealNutrition;

import java.util.List;

/**
 * Event published to RabbitMQ after nutrition data has been processed
 * from a CSV upload. The menu-management-service listens for this event
 * and persists the meals.
 */
public record MenuNutritionEvent(List<MealNutrition> meals, String jobId) {
    public MenuNutritionEvent(List<MealNutrition> meals) {
        this(meals, null);
    }
}
