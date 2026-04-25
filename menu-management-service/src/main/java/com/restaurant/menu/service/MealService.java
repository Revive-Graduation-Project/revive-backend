package com.restaurant.menu.service;

import com.restaurant.menu.dto.MealDTO;
import com.restaurant.menu.event.MenuNutritionEvent;

import java.util.List;

public interface MealService {

    /**
     * Processes the incoming nutrition event from RabbitMQ,
     * upserting meals and their ingredients.
     */
    void processNutritionEvent(MenuNutritionEvent event);

    /**
     * Retrieves all meals.
     */
    List<MealDTO> getAllMeals();

    /**
     * Retrieves a single meal by its ID.
     */
    MealDTO getMealById(Long id);
}
