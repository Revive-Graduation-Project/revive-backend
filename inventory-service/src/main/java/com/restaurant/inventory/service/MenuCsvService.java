package com.restaurant.inventory.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.restaurant.inventory.dto.IngredientEntry;
import com.restaurant.inventory.dto.MealNutrition;
import com.restaurant.inventory.dto.NormalizedIngredient;
import com.restaurant.inventory.dto.NutrientInfo;
import com.restaurant.inventory.dto.UsdaFoodDetail;
import com.restaurant.inventory.helper.CsvParserHelper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import com.restaurant.inventory.hooks.Genai;
import com.restaurant.inventory.hooks.UsdaService;

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
    private final Genai genai;
    private final UsdaService usdaService;

    public List<MealNutrition> getMenuNutritions(MultipartFile file) {
        csvParserHelper.validateFile(file);

        LinkedHashMap<String, List<IngredientEntry>> menuMap = csvParserHelper.parseMenu(file);
        List<MealNutrition> result = new ArrayList<>();

        menuMap.forEach((mealName, ingredients) -> {
            String prompt = ingredients.stream()
                    .map(IngredientEntry::name)
                    .collect(Collectors.joining(", "));

            String aiResponse = genai.aiMenuNormalizer(prompt);
            if (aiResponse == null || aiResponse.isBlank()) {
                log.error("Gemini returned null/empty response for meal '{}'", mealName);
                throw new RuntimeException("Gemini returned no response for meal: " + mealName);
            }
            ObjectMapper objectMapper = new ObjectMapper();

            try {
                List<NormalizedIngredient> normalized = objectMapper.readValue(
                        aiResponse,
                        new TypeReference<List<NormalizedIngredient>>() {
                        });

                Map<String, Double> quantityMap = ingredients.stream()
                        .collect(Collectors.toMap(
                                IngredientEntry::name,
                                IngredientEntry::quantity));

                List<UsdaFoodDetail> foodDetails = usdaService.fetchNutrients(normalized, quantityMap);

                // aggregate all ingredient nutrients into one meal total
                Map<String, NutrientInfo> aggregated = new LinkedHashMap<>();

                for (UsdaFoodDetail food : foodDetails) {
                    for (NutrientInfo nutrient : food.foodNutrients()) {
                        aggregated.merge(
                                nutrient.nutrientName() + "_" + nutrient.unitName(),
                                nutrient,
                                (existing, incoming) -> new NutrientInfo(
                                        existing.nutrientName(),
                                        Math.round((existing.value() + incoming.value()) * 100.0) / 100.0,
                                        existing.unitName()));
                    }
                }

                List<NutrientInfo> mealNutrients = new ArrayList<>(aggregated.values());
                result.add(new MealNutrition(mealName, mealNutrients));
                log.info("Aggregated nutrients for meal '{}'", mealName);

            } catch (com.fasterxml.jackson.core.JsonProcessingException e) {
                log.error("Failed to parse AI response for meal: {}", mealName, e);
                throw new RuntimeException("Failed to parse JSON from AI response", e);
            }
        });

        log.info("Processed {} meals from '{}'", result.size(), file.getOriginalFilename());
        return result;
    }
}