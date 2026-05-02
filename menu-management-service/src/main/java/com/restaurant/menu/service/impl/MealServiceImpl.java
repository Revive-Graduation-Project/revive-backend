package com.restaurant.menu.service.impl;

import com.restaurant.menu.dto.IngredientNutrition;
import com.restaurant.menu.dto.MealDTO;
import com.restaurant.menu.dto.MealNutrition;
import com.restaurant.menu.dto.NutrientInfo;
import com.restaurant.menu.entity.Ingredient;
import com.restaurant.menu.entity.Meal;
import com.restaurant.menu.entity.MealIngredient;
import com.restaurant.menu.event.MenuNutritionEvent;
import com.restaurant.menu.exception.MealNotFoundException;
import com.restaurant.menu.dto.MealRequest;
import com.restaurant.menu.mapper.MealMapper;
import com.restaurant.menu.repository.IngredientRepository;
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
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class MealServiceImpl implements MealService {

    private final MealRepository mealRepository;
    private final IngredientService ingredientService;
    private final IngredientRepository ingredientRepository;
    private final MealMapper mealMapper;

    @Override
    @Transactional
    public void processNutritionEvent(MenuNutritionEvent event) {
        log.info("Processing nutrition data for {} meals", event.meals().size());

        for (MealNutrition mealNutrition : event.meals()) {
            Map<String, NutrientInfo> aggregatedNutrients = new LinkedHashMap<>();

            // 1. Resolve Meal first (or prepare for creation)
            Meal meal = mealRepository.findByName(mealNutrition.mealName())
                    .orElseGet(() -> Meal.builder()
                            .name(mealNutrition.mealName())
                            .isActive(true)
                            .build());

            meal.setCategory(mealNutrition.category());
            meal.setPrice(mealNutrition.price());

            List<MealIngredient> mealIngredients = new ArrayList<>();

            for (IngredientNutrition ingDto : mealNutrition.ingredients()) {
                // Resolve Ingredient Entity
                Ingredient ingredient = ingredientService.resolveOrSaveIngredient(ingDto);

                mealIngredients.add(MealIngredient.builder()
                        .meal(meal)
                        .ingredient(ingredient)
                        .quantityGrams(ingDto.quantity())
                        .build());

                // Aggregate Nutrients
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

            meal.setNutrients(finalMealNutrients);

            // Set ingredients - orphanRemoval=true in Meal entity will handle old ones
            if (meal.getMealIngredients() != null) {
                meal.getMealIngredients().clear();
                meal.getMealIngredients().addAll(mealIngredients);
            } else {
                meal.setMealIngredients(mealIngredients);
            }

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

    @Override
    @Transactional(readOnly = true)
    public List<MealDTO> getMealsByIds(List<Long> ids) {
        log.info("Fetching meals by ids: {}", ids);
        return mealMapper.toDTOList(mealRepository.findAllById(ids));
    }

    @Override
    @Transactional
    public MealDTO createMeal(MealRequest request) {
        log.info("Creating meal: {}", request.name());

        Meal meal = Meal.builder()
                .name(request.name())
                .price(request.price())
                .category(request.category())
                .isActive(true)
                .build();

        return processMealIngredients(meal, request.ingredients());
    }

    @Override
    @Transactional
    public MealDTO updateMeal(Long id, MealRequest request) {
        log.info("Updating meal: {}", id);
        Meal meal = mealRepository.findById(id)
                .orElseThrow(() -> new MealNotFoundException(id));

        meal.setName(request.name());
        meal.setPrice(request.price());
        meal.setCategory(request.category());

        return processMealIngredients(meal, request.ingredients());
    }

    private MealDTO processMealIngredients(Meal meal, List<MealRequest.IngredientQuantity> ingredientRequests) {
        List<Long> ingredientIds = ingredientRequests.stream()
                .map(MealRequest.IngredientQuantity::ingredientId)
                .toList();

        List<Ingredient> ingredients = ingredientRepository.findAllById(ingredientIds);
        if (ingredients.size() != ingredientIds.size()) {
            throw new IllegalArgumentException("One or more ingredients not found");
        }

        Map<Long, Ingredient> ingredientMap = ingredients.stream()
                .collect(Collectors.toMap(Ingredient::getId, i -> i));

        List<MealIngredient> mealIngredients = new ArrayList<>();
        Map<String, NutrientInfo> aggregatedNutrients = new LinkedHashMap<>();

        for (MealRequest.IngredientQuantity req : ingredientRequests) {
            Ingredient ingredient = ingredientMap.get(req.ingredientId());
            mealIngredients.add(MealIngredient.builder()
                    .meal(meal)
                    .ingredient(ingredient)
                    .quantityGrams(req.quantity())
                    .build());

            if (ingredient.getNutrients() != null) {
                // We assume stored nutrients are absolute/per some unit.
                // We scale based on (quantity / 100.0) as standard USDA format is per 100g.
                double scalingFactor = req.quantity() / 100.0;

                for (Map<String, Object> nut : ingredient.getNutrients()) {
                    String nutrientName = (String) nut.get("nutrientName");
                    String unitName = (String) nut.get("unitName");
                    double value = nut.get("value") instanceof Number
                            ? ((Number) nut.get("value")).doubleValue()
                            : 0.0;

                    double scaledValue = Math.round(value * scalingFactor * 100.0) / 100.0;

                    aggregatedNutrients.merge(
                            nutrientName + "_" + unitName,
                            new NutrientInfo(nutrientName, scaledValue, unitName),
                            (existing, incoming) -> new NutrientInfo(
                                    existing.nutrientName(),
                                    Math.round((existing.value() + incoming.value()) * 100.0) / 100.0,
                                    existing.unitName()));
                }
            }
        }

        List<Map<String, Object>> finalMealNutrients = aggregatedNutrients.values().stream()
                .map(n -> Map.<String, Object>of(
                        "nutrientName", n.nutrientName(),
                        "value", n.value(),
                        "unitName", n.unitName()))
                .toList();

        meal.setNutrients(finalMealNutrients);

        if (meal.getMealIngredients() != null) {
            meal.getMealIngredients().clear();
            meal.getMealIngredients().addAll(mealIngredients);
        } else {
            meal.setMealIngredients(mealIngredients);
        }

        Meal saved = mealRepository.save(meal);
        return mealMapper.toDTO(saved);
    }
}
