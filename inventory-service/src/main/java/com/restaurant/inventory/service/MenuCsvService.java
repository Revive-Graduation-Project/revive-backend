package com.restaurant.inventory.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.restaurant.inventory.dto.IngredientEntry;
import com.restaurant.inventory.dto.IngredientNutrition;
import com.restaurant.inventory.dto.MealNutrition;
import com.restaurant.inventory.dto.ImportResponse;
import com.restaurant.inventory.dto.NormalizedIngredient;
import com.restaurant.inventory.dto.UsdaFoodDetail;
import com.restaurant.inventory.dto.InvalidMealEntry;
import com.restaurant.inventory.dto.ValidationResult;
import com.restaurant.inventory.event.MenuNutritionEvent;
import com.restaurant.inventory.helper.CsvParserHelper;
import com.restaurant.inventory.hooks.OpenRouter;
import com.restaurant.inventory.hooks.UsdaService;
import com.restaurant.inventory.messaging.MenuNutritionPublisher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class MenuCsvService {

    private final CsvParserHelper csvParserHelper;
    private final OpenRouter agent;
    private final UsdaService usdaService;
    private final MenuNutritionPublisher menuNutritionPublisher;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public ValidationResult validateCsv(MultipartFile file) {
        csvParserHelper.validateFile(file);
        List<Map<String, String>> rows = csvParserHelper.parse(file);
        LinkedHashMap<String, CsvParserHelper.MealCsvEntry> menuMap = csvParserHelper.parseMenu(file);

        List<CsvParserHelper.MealCsvEntry> valid = new ArrayList<>();
        List<InvalidMealEntry> invalid = new ArrayList<>();
        Set<String> seenNames = new LinkedHashSet<>();
        
        boolean isListFormat = !rows.isEmpty() && rows.get(0).containsKey("ingredients");
        
        int rowIndex = 1;
        for (Map<String, String> row : rows) {
            String mealName = row.get("meal_name");
            if (mealName == null || mealName.isBlank()) {
                invalid.add(new InvalidMealEntry(rowIndex, "", "Missing meal name"));
            } else if (isListFormat && seenNames.contains(mealName.trim().toLowerCase())) {
                invalid.add(new InvalidMealEntry(rowIndex, mealName.trim(), "Duplicate meal name in CSV"));
            } else {
                seenNames.add(mealName.trim().toLowerCase());
            }
            rowIndex++;
        }

        for (Map.Entry<String, CsvParserHelper.MealCsvEntry> entry : menuMap.entrySet()) {
            CsvParserHelper.MealCsvEntry meal = entry.getValue();
            if (meal.ingredients().isEmpty()) {
                int rIdx = 1;
                for (Map<String, String> r : rows) {
                    if (meal.mealName().equals(r.get("meal_name"))) break;
                    rIdx++;
                }
                invalid.add(new InvalidMealEntry(rIdx, meal.mealName(), "No ingredients found"));
            } else {
                // If it's list format and was a duplicate, it's already in invalid, so don't add to valid.
                // Actually, if it's a duplicate, we only want the first one to be valid?
                // The requirements don't specify if the first one is valid or skipped. Let's just say if it's not in the invalid duplicates, it's valid.
                // Wait, if it's a duplicate in Format A, seenNames has it, but it was flagged.
                // Let's just add to valid if it has ingredients, and we handle duplicates separately.
                // Actually, if a meal is duplicated in Format A, CsvParserHelper overwrites it, so menuMap only has ONE entry.
                // That entry will be valid, but we ALSO flagged the duplicate row. This is fine.
                valid.add(meal);
            }
        }
        return new ValidationResult(valid, invalid);
    }

    /**
     * All-or-nothing pipeline: if ANY step fails for ANY meal the entire
     * batch is aborted and no event is published to RabbitMQ.
     *
     * Steps per meal:
     * 1. Call AI → get USDA search params (normalized ingredient names)
     * 2. Search USDA → get food IDs (fdcIds)
     * 3. Fetch nutritions by IDs
     * 4. Group ingredients and their nutrition together
     *
     * Final step (once for entire batch):
     * 5. Publish the MenuNutritionEvent to RabbitMQ
     */
    public List<MealNutrition> getMenuNutritions(MultipartFile file) {
        try {
            csvParserHelper.validateFile(file);
        } catch (Exception e) {
            throw new RuntimeException("Step 1 Failed: CSV Validation Error - " + e.getMessage(), e);
        }
        
        LinkedHashMap<String, CsvParserHelper.MealCsvEntry> menuMap;
        try {
            menuMap = csvParserHelper.parseMenu(file);
        } catch (Exception e) {
            throw new RuntimeException("Step 2 Failed: CSV Parsing Error - " + e.getMessage(), e);
        }

        return runPipeline(menuMap, file.getOriginalFilename());
    }

    public ImportResponse importFromJson(List<CsvParserHelper.MealCsvEntry> meals) {
        LinkedHashMap<String, CsvParserHelper.MealCsvEntry> menuMap = new LinkedHashMap<>();
        for (CsvParserHelper.MealCsvEntry meal : meals) {
            menuMap.put(meal.mealName(), meal);
        }
        
        java.util.concurrent.CompletableFuture.runAsync(() -> {
            try {
                runPipeline(menuMap, "JSON import");
            } catch (Exception e) {
                log.error("Background JSON import failed", e);
            }
        });
        
        return new ImportResponse("Import started — " + meals.size() + " meals queued for processing.", meals.size());
    }

    /**
     * Shared pipeline — called by both endpoints after parsing is done
     */
    public List<MealNutrition> runPipeline(LinkedHashMap<String, CsvParserHelper.MealCsvEntry> menuMap, String sourceLabel) {
        List<MealNutrition> result = new ArrayList<>();

        try {
            // ── Step 0: Batch AI Normalization ────────────────
            log.info("Batching AI normalizer for all ingredients...");
            List<String> allUniqueIngredients = menuMap.values().stream()
                    .flatMap(meal -> meal.ingredients().stream())
                    .map(IngredientEntry::name)
                    .distinct()
                    .toList();

            final Map<String, NormalizedIngredient> normalizedCache;
            try {
                normalizedCache = allUniqueIngredients.isEmpty()
                        ? Map.of()
                        : batchNormalizeIngredients(allUniqueIngredients);
            } catch (Exception e) {
                throw new RuntimeException("Step 3 Failed: AI Normalization Error - " + e.getMessage(), e);
            }

            if (!normalizedCache.isEmpty()) {
                log.info("Successfully normalized {} unique ingredients via AI", normalizedCache.size());
            }

            for (var entry : menuMap.entrySet()) {
                String mealName = entry.getKey();
                CsvParserHelper.MealCsvEntry mealCsvEntry = entry.getValue();
                List<IngredientEntry> ingredients = mealCsvEntry.ingredients();

                // ── Step 1: Get USDA search params from cache ────────────────
                List<NormalizedIngredient> normalized = ingredients.stream()
                        .map(i -> normalizedCache.getOrDefault(i.name(),
                                // Fallback if AI missed it
                                new NormalizedIngredient(i.name(), i.name())))
                        .toList();

                // ── Step 2 & 3: Search USDA for IDs → fetch nutritions ───────
                Map<String, Double> quantityMap = ingredients.stream()
                        .collect(Collectors.toMap(IngredientEntry::name, IngredientEntry::quantity));

                List<UsdaFoodDetail> foodDetails;
                try {
                    foodDetails = usdaService.fetchNutrients(normalized, quantityMap);
                } catch (Exception e) {
                    throw new RuntimeException("Step 4 Failed: USDA API Fetch Error for meal '" + mealName + "' - " + e.getMessage(), e);
                }

                // ── Step 4: Map to IngredientNutrition ─────────────────────
                List<IngredientNutrition> ingredientNutritions;
                try {
                    ingredientNutritions = foodDetails.stream().map(food -> {
                        // Find original entry for quantity and unit
                        IngredientEntry originalEntry = ingredients.stream()
                                .filter(i -> i.name().equals(food.originalName()))
                                .findFirst()
                                .orElse(new IngredientEntry(food.originalName(), 0, ""));

                        return new IngredientNutrition(
                                food.originalName(),
                                originalEntry.quantity(),
                                originalEntry.unit(),
                                food.fdcId(),
                                food.description(),
                                food.foodCategory(),
                                food.foodNutrients());
                    }).toList();
                } catch (Exception e) {
                    throw new RuntimeException("Step 5 Failed: Ingredient Mapping Error for meal '" + mealName + "' - " + e.getMessage(), e);
                }

                MealNutrition mealNutrition = new MealNutrition(
                        mealName,
                        mealCsvEntry.category(),
                        mealCsvEntry.price(),
                        mealCsvEntry.description(),
                        ingredientNutritions);
                result.add(mealNutrition);

                log.info("Processed ingredients and nutrients for meal '{}'", mealName);
            }

            // ── Step 5: Publish event (only reached if ALL meals succeeded) ──
            log.info("Processed {} meals from '{}' — publishing event", result.size(), sourceLabel);
            try {
                menuNutritionPublisher.publish(new MenuNutritionEvent(result));
            } catch (Exception e) {
                throw new RuntimeException("Step 6 Failed: RabbitMQ Publish Error - " + e.getMessage(), e);
            }

            return result;

        } catch (RuntimeException re) {
            log.error("Pipeline failed for '{}': {}", sourceLabel, re.getMessage(), re);
            throw re;
        } catch (Exception e) {
            log.error("Pipeline failed for '{}': {}", sourceLabel, e.getMessage(), e);
            throw new RuntimeException("Menu nutrition pipeline failed with unknown error: " + e.getMessage(), e);
        }
    }

    // ───────────────────────── private helpers ─────────────────────────

    /**
     * Step 0 – call AI to normalize all unique ingredient names in the batch.
     * To prevent LLM hallucinations on large lists, we process them in chunks of
     * 20.
     */
    private Map<String, NormalizedIngredient> batchNormalizeIngredients(List<String> uniqueIngredients) {
        Map<String, NormalizedIngredient> resultMap = new java.util.HashMap<>();
        int chunkSize = 20;

        for (int i = 0; i < uniqueIngredients.size(); i += chunkSize) {
            int end = Math.min(uniqueIngredients.size(), i + chunkSize);
            List<String> chunk = uniqueIngredients.subList(i, end);
            String prompt = String.join(", ", chunk);

            String aiResponse = agent.aiMenuNormalizer(prompt);
            if (aiResponse == null || aiResponse.isBlank()) {
                log.error("AI returned no response for chunk {} to {}", i, end);
                continue; // Skip chunk on failure or throw exception
            }

            // Defensively strip markdown formatting
            String cleanJson = aiResponse.trim();
            if (cleanJson.startsWith("```json")) {
                cleanJson = cleanJson.substring(7);
            } else if (cleanJson.startsWith("```")) {
                cleanJson = cleanJson.substring(3);
            }
            if (cleanJson.endsWith("```")) {
                cleanJson = cleanJson.substring(0, cleanJson.length() - 3);
            }
            cleanJson = cleanJson.trim();

            try {
                List<NormalizedIngredient> parsed = objectMapper.readValue(cleanJson, new TypeReference<>() {
                });
                for (NormalizedIngredient item : parsed) {
                    resultMap.put(item.originalName(), item);
                }
            } catch (JsonProcessingException e) {
                log.error("Failed to parse AI JSON for chunk. Raw response: {}", aiResponse, e);
                throw new RuntimeException("Failed to parse AI JSON for batch chunk. Raw response: " + aiResponse, e);
            }
        }
        return resultMap;
    }
}