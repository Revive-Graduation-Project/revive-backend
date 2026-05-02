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

    /**
     * Retrieves multiple meals by their IDs in a single query.
     * Any ID not found in the database is silently omitted from the result.
     *
     * @param ids list of meal IDs to fetch
     * @return list of matching MealDTOs
     */
    List<MealDTO> getMealsByIds(List<Long> ids);
    /**
     * Creates a new meal from a manual request.
     */
    MealDTO createMeal(com.restaurant.menu.dto.MealRequest request);

    /**
     * Updates an existing meal from a manual request.
     */
    MealDTO updateMeal(Long id, com.restaurant.menu.dto.MealRequest request);

    /**
     * Deletes an existing meal.
     */
    void deleteMeal(Long id);
}

