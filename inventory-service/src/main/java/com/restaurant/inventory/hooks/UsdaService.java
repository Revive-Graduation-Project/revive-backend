package com.restaurant.inventory.hooks;

import com.restaurant.inventory.dto.NormalizedIngredient;
import com.restaurant.inventory.dto.NutrientInfo;
import com.restaurant.inventory.dto.UsdaFoodDetail;
import com.restaurant.inventory.dto.UsdaSearchResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

@Slf4j
@Service
public class UsdaService {

        private final WebClient webClient;

        @Value("${usda.api.key}")
        private String apiKey;

        private static final List<String> RELEVANT_NUTRIENTS = List.of(
                        "Energy",
                        "Protein",
                        "Total lipid (fat)",
                        "Fatty acids, total saturated",
                        "Carbohydrate, by difference",
                        "Total Sugars",
                        "Fiber, total dietary",
                        "Sodium, Na",
                        "Cholesterol");

        public UsdaService(@Value("${usda.api.url}") String baseUrl) {
                this.webClient = WebClient.builder()
                                .baseUrl(baseUrl)
                                .build();
        }

        public List<UsdaFoodDetail> fetchNutrients(List<NormalizedIngredient> ingredients,
                        Map<String, Double> quantityMap) {
                // Step 1 - search all in parallel to get fdcIds
                List<CompletableFuture<UsdaSearchResult>> futures = ingredients.stream()
                                .map(ingredient -> CompletableFuture.supplyAsync(() -> search(ingredient)))
                                .toList();

                List<UsdaSearchResult> searchResults = futures.stream()
                                .map(CompletableFuture::join)
                                .toList();

                // Step 2 - bulk fetch nutrients by fdcIds
                List<Integer> fdcIds = searchResults.stream()
                                .filter(r -> r.fdcId() != -1)
                                .map(UsdaSearchResult::fdcId)
                                .toList();

                if (fdcIds.isEmpty()) {
                        log.warn("No valid fdcIds to bulk fetch");
                        return List.of();
                }

                Map<String, Object> requestBody = Map.of(
                                "fdcIds", fdcIds,
                                "format", "abridged");

                List<Map> response = webClient.post()
                                .uri(uriBuilder -> uriBuilder
                                                .path("/foods")
                                                .queryParam("api_key", apiKey)
                                                .build())
                                .header("Content-Type", "application/json")
                                .bodyValue(requestBody)
                                .retrieve()
                                .bodyToFlux(Map.class)
                                .collectList()
                                .block();

                if (response == null) {
                        log.warn("USDA bulk fetch returned null");
                        return List.of();
                }

                // Step 3 - map response by fdcId so we can reorder correctly
                Map<Integer, Map> foodById = response.stream()
                                .collect(Collectors.toMap(
                                                food -> (int) food.get("fdcId"),
                                                food -> food));

                // Step 4 - rebuild in the same order as the original ingredients
                return searchResults.stream()
                                .filter(r -> r.fdcId() != -1)
                                .map(searchResult -> {
                                        Map food = foodById.get(searchResult.fdcId());
                                        if (food == null) {
                                                log.warn("No food data returned for fdcId {}", searchResult.fdcId());
                                                return null;
                                        }

                                        String description = (String) food.get("description");

                                        // quantity in grams for this ingredient — used to scale nutrients from per 100g
                                        double quantity = quantityMap.getOrDefault(searchResult.originalName(), 100.0);
                                        double scalingFactor = quantity / 100.0;

                                        List<Map> rawNutrients = (List<Map>) food.get("foodNutrients");
                                        List<NutrientInfo> nutrients = rawNutrients == null ? List.of()
                                                        : rawNutrients.stream()
                                                                        .map(n -> {
                                                                                String nutrientName = (String) n
                                                                                                .get("name");
                                                                                String unitName = (String) n
                                                                                                .get("unitName");
                                                                                double value = n.get(
                                                                                                "amount") instanceof Number
                                                                                                                ? ((Number) n.get(
                                                                                                                                "amount"))
                                                                                                                                .doubleValue()
                                                                                                                : 0.0;
                                                                                // scale value to actual quantity used
                                                                                double scaledValue = Math.round(value
                                                                                                * scalingFactor * 100.0)
                                                                                                / 100.0;
                                                                                return new NutrientInfo(nutrientName,
                                                                                                scaledValue, unitName);
                                                                        })
                                                                        .filter(n -> n.nutrientName() != null)
                                                                        .filter(n -> RELEVANT_NUTRIENTS
                                                                                        .contains(n.nutrientName()))
                                                                        .filter(n -> !(n.nutrientName().equals("Energy")
                                                                                        && n.unitName().equals("kJ")))
                                                                        .toList();

                                        return new UsdaFoodDetail(searchResult.fdcId(), description, nutrients);
                                })
                                .filter(f -> f != null)
                                .toList();
        }

        private UsdaSearchResult search(NormalizedIngredient ingredient) {
                Map response = webClient.get()
                                .uri(uriBuilder -> uriBuilder
                                                .path("/foods/search")
                                                .queryParam("api_key", apiKey)
                                                .queryParam("query", ingredient.query())
                                                .queryParam("dataType", "Foundation,SR Legacy")
                                                .queryParam("pageSize", 1)
                                                .build())
                                .retrieve()
                                .bodyToMono(Map.class)
                                .block();

                List<Map> foods = (List<Map>) response.get("foods");

                if (foods == null || foods.isEmpty()) {
                        log.warn("No USDA result found for query '{}'", ingredient.query());
                        return new UsdaSearchResult(ingredient.originalName(), -1);
                }

                int fdcId = (int) foods.get(0).get("fdcId");
                log.debug("Found fdcId {} for ingredient '{}'", fdcId, ingredient.originalName());

                return new UsdaSearchResult(ingredient.originalName(), fdcId);
        }
}