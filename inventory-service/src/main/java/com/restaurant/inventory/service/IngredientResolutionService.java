package com.restaurant.inventory.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.restaurant.inventory.dto.IngredientEntry;
import com.restaurant.inventory.dto.IngredientNutrition;
import com.restaurant.inventory.dto.NormalizedIngredient;
import com.restaurant.inventory.dto.UsdaFoodDetail;
import com.restaurant.inventory.hooks.OpenRouter;
import com.restaurant.inventory.hooks.UsdaService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class IngredientResolutionService {

    private final OpenRouter agent;
    private final UsdaService usdaService;
    private final ObjectMapper objectMapper;

    public List<IngredientNutrition> resolveIngredients(List<IngredientEntry> ingredients) {
        log.info("Resolving {} ingredients via AI and USDA", ingredients.size());

        List<String> uniqueIngredientNames = ingredients.stream()
                .map(IngredientEntry::name)
                .distinct()
                .toList();

        final Map<String, NormalizedIngredient> normalizedCache = uniqueIngredientNames.isEmpty()
                ? Map.of()
                : batchNormalizeIngredients(uniqueIngredientNames);

        List<NormalizedIngredient> normalized = ingredients.stream()
                .map(i -> normalizedCache.getOrDefault(i.name(),
                        new NormalizedIngredient(i.name(), i.name())))
                .toList();

        Map<String, Double> quantityMap = ingredients.stream()
                .collect(Collectors.toMap(IngredientEntry::name, IngredientEntry::quantity, (a, b) -> a)); // handling duplicate names gracefully

        List<UsdaFoodDetail> foodDetails = usdaService.fetchNutrients(normalized, quantityMap);

        return ingredients.stream().map(originalEntry -> {
            UsdaFoodDetail food = foodDetails.stream()
                    .filter(f -> f.originalName().equals(originalEntry.name()))
                    .findFirst()
                    .orElse(null);

            if (food == null) {
                return new IngredientNutrition(originalEntry.name(), originalEntry.quantity(), originalEntry.unit(), null, "", "", null);
            }

            return new IngredientNutrition(
                    food.originalName(),
                    originalEntry.quantity(),
                    originalEntry.unit(),
                    food.fdcId(),
                    food.description(),
                    food.foodCategory(),
                    food.foodNutrients());
        }).toList();
    }

    public Map<String, NormalizedIngredient> batchNormalizeIngredients(List<String> uniqueIngredients) {
        Map<String, NormalizedIngredient> resultMap = new java.util.HashMap<>();
        int chunkSize = 20;

        for (int i = 0; i < uniqueIngredients.size(); i += chunkSize) {
            int end = Math.min(uniqueIngredients.size(), i + chunkSize);
            List<String> chunk = uniqueIngredients.subList(i, end);
            String prompt = String.join(", ", chunk);

            String aiResponse = agent.aiMenuNormalizer(prompt);
            if (aiResponse == null || aiResponse.isBlank()) {
                log.error("AI returned no response for chunk {} to {}", i, end);
                continue;
            }

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
                List<NormalizedIngredient> parsed = objectMapper.readValue(cleanJson, new TypeReference<>() {});
                for (NormalizedIngredient item : parsed) {
                    resultMap.put(item.originalName(), item);
                }
            } catch (JsonProcessingException e) {
                log.error("Failed to parse AI JSON for chunk {} to {}. Raw response: {}", i, end, aiResponse, e);
                // Skip chunk; callers fall back to original names via getOrDefault
            }
        }
        return resultMap;
    }
}
