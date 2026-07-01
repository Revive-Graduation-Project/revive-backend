package com.restaurant.menu.service;

import com.restaurant.menu.dto.DiscountRequest;
import com.restaurant.menu.dto.MealDTO;
import com.restaurant.menu.dto.MealRequest;
import com.restaurant.menu.event.MenuNutritionEvent;
import org.springframework.web.multipart.MultipartFile;

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
    MealDTO createMeal(MealRequest request);

    /**
     * Updates an existing meal from a manual request.
     */
    MealDTO updateMeal(Long id, MealRequest request);

    /**
     * Deletes an existing meal.
     */
    void deleteMeal(Long id);

    /**
     * Updates the discount status and percentage for a meal.
     */
    MealDTO updateDiscount(Long id, DiscountRequest request);

    /**
     * Retrieves meals filtered by discount status.
     * If hasDiscount is null, returns all meals.
     */
    List<MealDTO> getMealsByDiscount(Boolean hasDiscount);

    /**
     * Uploads an image for a meal and returns the public URL.
     */
    String uploadMealImage(Long id, MultipartFile file);

    /**
     * Uploads multiple images for meals, using filename convention (e.g. 5.jpg) for IDs.
     */
    List<String> uploadBulkMealImages(MultipartFile[] files);

    /**
     * Deletes the image for a meal.
     */
    void deleteMealImage(Long id);
}
