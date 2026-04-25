package com.restaurant.menu.service.impl;

import com.restaurant.menu.dto.IngredientNutrition;
import com.restaurant.menu.dto.MealDTO;
import com.restaurant.menu.dto.MealNutrition;
import com.restaurant.menu.dto.NutrientInfo;
import com.restaurant.menu.entity.Ingredient;
import com.restaurant.menu.entity.Meal;
import com.restaurant.menu.event.MenuNutritionEvent;
import com.restaurant.menu.exception.MealNotFoundException;
import com.restaurant.menu.mapper.MealMapper;
import com.restaurant.menu.repository.MealRepository;
import com.restaurant.menu.service.IngredientService;
import com.restaurant.menu.service.MealService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@Slf4j
@RequiredArgsConstructor
public class MealServiceImpl implements MealService {

    private final MealRepository mealRepository;
    private final IngredientService ingredientService;
    private final MealMapper mealMapper;

    @Override
    @Transactional
    public void processNutritionEvent(MenuNutritionEvent event) {
        log.info("Processing nutrition data for {} meals", event.meals().size());

        for (MealNutrition mealNutrition : event.meals()) {
            
            List<Ingredient> mealIngredients = new ArrayList<>();
            Map<String, NutrientInfo> aggregatedNutrients = new LinkedHashMap<>();

            for (IngredientNutrition ingDto : mealNutrition.ingredients()) {
                // 1. Resolve Ingredient Entity via the IngredientService
                Ingredient ingredient = ingredientService.resolveOrSaveIngredient(ingDto);
                mealIngredients.add(ingredient);

                // 2. Aggregate Nutrients for the Meal
                for (NutrientInfo nutrient : ingDto.nutrients()) {
                    aggregatedNutrients.merge(
                            nutrient.nutrientName() + "_" + nutrient.unitName(),
                            nutrient,
                            (existing, incoming) -> new NutrientInfo(
                                    existing.nutrientName(),
                                    Math.round((existing.value() + incoming.value()) * 100.0) / 100.0,
                                    existing.unitName()));
                }
            }

            // Convert aggregated to List<Map>
            List<Map<String, Object>> finalMealNutrients = aggregatedNutrients.values().stream()
                    .map(n -> Map.<String, Object>of(
                            "nutrientName", n.nutrientName(),
                            "value", n.value(),
                            "unitName", n.unitName()))
                    .toList();

            // 3. Upsert Meal
            Meal meal = mealRepository.findByName(mealNutrition.mealName())
                    .map(existing -> {
                        existing.setNutrients(finalMealNutrients);
                        existing.setIngredients(mealIngredients);
                        existing.setCategory(mealNutrition.category());
                        existing.setPrice(mealNutrition.price());
                        log.info("Updating existing meal: '{}'", mealNutrition.mealName());
                        return existing;
                    })
                    .orElseGet(() -> {
                        log.info("Creating new meal: '{}'", mealNutrition.mealName());
                        return Meal.builder()
                                .name(mealNutrition.mealName())
                                .nutrients(finalMealNutrients)
                                .ingredients(mealIngredients)
                                .price(mealNutrition.price())
                                .category(mealNutrition.category())
                                .isActive(true)
                                .build();
                    });

            mealRepository.save(meal);
        }

        log.info("Successfully persisted {} meals", event.meals().size());
    }

    @Override
    @Transactional(readOnly = true)
    public List<MealDTO> getAllMeals() {
        log.info("Fetching all meals");
        List<Meal> meals = mealRepository.findAll();
        return mealMapper.toDTOList(meals);
    }

    @Override
    @Transactional(readOnly = true)
    public MealDTO getMealById(Long id) {
        log.info("Fetching meal with id: {}", id);
        Meal meal = mealRepository.findById(id)
                .orElseThrow(() -> new MealNotFoundException(id));
        return mealMapper.toDTO(meal);
    }
}
