package com.restaurant.menu.client;

import com.restaurant.menu.dto.IngredientEntry;
import com.restaurant.menu.dto.IngredientNutrition;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.List;

import org.springframework.retry.annotation.Retryable;
import org.springframework.retry.annotation.Backoff;

@Component
@Slf4j
public class InventoryClient {

    private final RestClient restClient;

    public InventoryClient(RestClient.Builder restClientBuilder,
                           @Value("${app.services.inventory-url}") String inventoryBaseUrl) {
        this.restClient = restClientBuilder
                .baseUrl(inventoryBaseUrl)
                .build();
    }

    @Retryable(retryFor = RuntimeException.class, maxAttempts = 3, backoff = @Backoff(delay = 1000))
    public List<IngredientNutrition> resolveIngredients(List<IngredientEntry> ingredients) {
        log.info("Requesting inventory-service to resolve {} missing ingredients", ingredients.size());

        return restClient.post()
                .uri("/api/inventory/ingredients/resolve")
                .body(ingredients)
                .retrieve()
                .onStatus(HttpStatusCode::isError, (request, response) -> {
                    log.error("Failed to resolve ingredients in inventory-service. Status: {}", response.getStatusCode());
                    throw new RuntimeException("Failed to resolve missing ingredients in inventory-service");
                })
                .body(new ParameterizedTypeReference<List<IngredientNutrition>>() {});
    }
}
