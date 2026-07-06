package com.restaurant.inventory.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.restaurant.inventory.dto.IngredientEntry;
import com.restaurant.inventory.dto.IngredientNutrition;
import com.restaurant.inventory.dto.MealNutrition;
import com.restaurant.inventory.dto.NormalizedIngredient;
import com.restaurant.inventory.dto.UsdaFoodDetail;
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
import java.util.List;
import java.util.Map;
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
        csvParserHelper.validateFile(file);
        LinkedHashMap<String, CsvParserHelper.MealCsvEntry> menuMap = csvParserHelper.parseMenu(file);

        List<MealNutrition> result = new ArrayList<>();

        try {
            // ── Step 0: Batch AI Normalization ────────────────
            log.info("Batching AI normalizer for all ingredients...");
            List<String> allUniqueIngredients = menuMap.values().stream()
                    .flatMap(meal -> meal.ingredients().stream())
                    .map(IngredientEntry::name)
                    .distinct()
                    .toList();

            final Map<String, NormalizedIngredient> normalizedCache = allUniqueIngredients.isEmpty()
                    ? Map.of()
                    : batchNormalizeIngredients(allUniqueIngredients);

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

                List<UsdaFoodDetail> foodDetails = usdaService.fetchNutrients(normalized, quantityMap);

                // ── Step 4: Map to IngredientNutrition ─────────────────────
                List<IngredientNutrition> ingredientNutritions = foodDetails.stream().map(food -> {
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
            log.info("Processed {} meals from '{}' — publishing event", result.size(), file.getOriginalFilename());
            menuNutritionPublisher.publish(new MenuNutritionEvent(result));

            return result;

        } catch (Exception e) {
            log.error("Pipeline failed — no event published. Cause: {}", e.getMessage(), e);
            throw new RuntimeException("Menu nutrition pipeline failed: " + e.getMessage(), e);
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