package com.restaurant.menu.service.impl;

import com.restaurant.menu.dto.BulkUpdateStockRequest.StockEntry;
import com.restaurant.menu.dto.IngredientDTO;
import com.restaurant.menu.dto.IngredientNutrition;
import com.restaurant.menu.dto.MealDTO;
import com.restaurant.menu.dto.MealIngredientDTO;
import com.restaurant.menu.dto.ReserveMealDTO;
import com.restaurant.menu.entity.Ingredient;
import com.restaurant.menu.entity.Meal;
import com.restaurant.menu.exception.IngredientNotFoundException;
import com.restaurant.menu.exception.InsufficientStockException;
import com.restaurant.menu.mapper.IngredientMapper;
import com.restaurant.menu.mapper.MealMapper;
import com.restaurant.menu.repository.IngredientRepository;
import com.restaurant.menu.repository.MealRepository;
import com.restaurant.menu.service.IngredientService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class IngredientServiceImpl implements IngredientService {

    private final IngredientRepository ingredientRepository;
    private final IngredientMapper ingredientMapper;
    private final MealRepository mealRepository;
    private final MealMapper mealMapper;

    @Override
    @Transactional
    public Ingredient resolveOrSaveIngredient(IngredientNutrition ingDto) {
        return ingredientRepository.findByName(ingDto.ingredientName())
                .orElseGet(() -> {
                    log.info("Creating new ingredient: '{}'", ingDto.ingredientName());
                    List<Map<String, Object>> ingNutrients = ingDto.nutrients().stream()
                            .map(n -> Map.<String, Object>of(
                                    "nutrientName", n.nutrientName(),
                                    "value", n.value(),
                                    "unitName", n.unitName()))
                            .toList();

                    Ingredient newIng = Ingredient.builder()
                            .name(ingDto.ingredientName())
                            .description(ingDto.description())
                            .nutrients(ingNutrients)
                            .build();
                    return ingredientRepository.save(newIng);
                });
    }

    @Override
    @Transactional(readOnly = true)
    public List<IngredientDTO> getAllIngredients() {
        log.info("Fetching all ingredients");
        List<Ingredient> ingredients = ingredientRepository.findAll();
        return ingredientMapper.toDTOList(ingredients);
    }

    @Override
    @Transactional(readOnly = true)
    public IngredientDTO getIngredientById(Long id) {
        log.info("Fetching ingredient with id: {}", id);
        Ingredient ingredient = ingredientRepository.findById(id)
                .orElseThrow(() -> new IngredientNotFoundException(id));
        return ingredientMapper.toDTO(ingredient);
    }

    @Override
    @Transactional
    public IngredientDTO updateStock(Long id, Double stock) {
        log.info("Updating stock for ingredient id: {} to {}", id, stock);
        Ingredient ingredient = ingredientRepository.findById(id)
                .orElseThrow(() -> new IngredientNotFoundException(id));
        ingredient.setStock(stock);
        ingredientRepository.save(ingredient);
        return ingredientMapper.toDTO(ingredient);
    }

    @Override
    @Transactional
    public List<IngredientDTO> bulkUpdateStock(List<StockEntry> updates) {
        log.info("Bulk updating stock for {} ingredients", updates.size());

        List<Long> ids = updates.stream().map(StockEntry::ingredientId).toList();
        Map<Long, Ingredient> ingredientMap = ingredientRepository.findAllById(ids).stream()
                .collect(Collectors.toMap(Ingredient::getId, i -> i));

        // Validate all IDs exist before applying any changes
        for (Long id : ids) {
            if (!ingredientMap.containsKey(id)) {
                throw new IngredientNotFoundException(id);
            }
        }

        // Apply stock updates
        for (var entry : updates) {
            Ingredient ingredient = ingredientMap.get(entry.ingredientId());
            ingredient.setStock(entry.stock());
        }

        List<Ingredient> saved = ingredientRepository.saveAll(ingredientMap.values().stream().toList());
        return ingredientMapper.toDTOList(saved);
    }

    // -------------------------------------------------------------------------
    // Stock Reservation (used by Order-service via REST)
    // -------------------------------------------------------------------------

    @Override
    @Transactional
    public List<IngredientDTO> reserveIngredients(List<ReserveMealDTO> meals) {
        log.info("Reserving ingredients for {} meal line(s)", meals.size());

        // 1. Aggregate required deduction per ingredient across all meals × quantity
        // e.g. 2 milkshakes + 1 burger → sum up all ingredient needs
        Map<Long, Double> deductions = buildIngredientDeductions(meals);

        // 2. Fetch and lock ingredient rows — blocks concurrent orders from reading
        // stale stock values (SELECT ... FOR UPDATE)
        List<Long> ingredientIds = new ArrayList<>(deductions.keySet());
        Map<Long, Ingredient> locked = ingredientRepository
                .findAllByIdWithLock(ingredientIds)
                .stream()
                .collect(Collectors.toMap(Ingredient::getId, i -> i));

        // 3. Validate sufficient stock before touching anything
        for (Map.Entry<Long, Double> entry : deductions.entrySet()) {
            Ingredient ing = locked.get(entry.getKey());
            if (ing == null)
                throw new IngredientNotFoundException(entry.getKey());
            if (ing.getStock() < entry.getValue()) {
                throw new InsufficientStockException(ing.getName(), ing.getStock(), entry.getValue());
            }
        }

        // 4. Deduct stock and persist
        deductions.forEach((id, qty) -> locked.get(id).setStock(locked.get(id).getStock() - qty));
        List<Ingredient> saved = ingredientRepository.saveAll(locked.values());

        log.info("Stock reserved successfully for {} ingredients", saved.size());
        return ingredientMapper.toDTOList(saved);
    }

    @Override
    @Transactional
    public void revertIngredients(List<ReserveMealDTO> meals) {
        log.info("Reverting ingredient stock for {} meal line(s)", meals.size());

        // No pessimistic lock needed here — adding stock back is always safe
        Map<Long, Double> deductions = buildIngredientDeductions(meals);
        List<Long> ingredientIds = new ArrayList<>(deductions.keySet());
        Map<Long, Ingredient> ingredients = ingredientRepository
                .findAllById(ingredientIds)
                .stream()
                .collect(Collectors.toMap(Ingredient::getId, i -> i));

        deductions.forEach((id, qty) -> {
            Ingredient ing = ingredients.get(id);
            if (ing != null)
                ing.setStock(ing.getStock() + qty);
        });
        ingredientRepository.saveAll(ingredients.values());

        log.info("Stock reverted successfully for {} ingredients", ingredients.size());
    }

    /**
     * Aggregates ingredient stock deductions across all meal line items.
     * For each meal, multiplies the ingredient count by the ordered quantity.
     *
     * NOTE: The current Meal→Ingredient relationship is a plain @ManyToMany
     * with no per-ingredient quantity on the join table. Each ingredient
     * is therefore counted as 1 unit per meal quantity ordered.
     * Add a quantity column to the meal_ingredients join table and update
     * this method when precise ingredient amounts are available.
     *
     * @param meals list of meal reservations (mealId + ordered quantity)
     * @return map of ingredientId -> total units to deduct
     */
    private Map<Long, Double> buildIngredientDeductions(List<ReserveMealDTO> meals) {
        List<Long> mealIds = meals.stream().map(ReserveMealDTO::mealId).toList();
        Map<Long, Integer> quantityByMealId = meals.stream()
                .collect(Collectors.toMap(ReserveMealDTO::mealId, ReserveMealDTO::quantity));

        List<Meal> mealsEntities = mealRepository.findAllById(mealIds);
        List<MealDTO> mealDTOs = mealMapper.toDTOList(mealsEntities);

        Map<Long, Double> deductions = new HashMap<>();
        for (MealDTO meal : mealDTOs) {
            double orderedQty = quantityByMealId.getOrDefault(meal.id(), 0);
            if (meal.mealIngredients() == null) continue;
            for (MealIngredientDTO mi : meal.mealIngredients()) {
                // multiply the grams per serving by the number of meals ordered
                double gramsNeeded = mi.quantityGrams() * orderedQty;
                deductions.merge(mi.ingredient().id(), gramsNeeded, Double::sum);
            }
        }
        return deductions;
    }
}
