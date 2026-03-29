package com.restaurant.order.client;

import com.restaurant.order.dto.snapshot.CustomIngredientSnapshot;
import com.restaurant.order.exception.InventoryServiceException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.Map;
import java.util.UUID;

@Component
@Slf4j
public class InventoryServiceClient implements InventoryClient {

    private final RestClient restClient;

    public InventoryServiceClient(RestClient.Builder restClientBuilder,
                                  @Value("${app.services.inventory-url}") String inventoryBaseUrl) {
        this.restClient = restClientBuilder
                .baseUrl(inventoryBaseUrl)
                .build();
    }

    @Override
    public CustomIngredientSnapshot getIngredientById(UUID ingredientId) {
        log.info("Fetching ingredient snapshot from inventory-service for ingredientId: {}", ingredientId);

        return restClient.get()
                .uri("/api/inventory/ingredients/{ingredientId}", ingredientId)
                .retrieve()
                .onStatus(HttpStatusCode::is4xxClientError, (request, response) -> {
                    throw new InventoryServiceException("Ingredient not found with ID: " + ingredientId);
                })
                .onStatus(HttpStatusCode::is5xxServerError, (request, response) -> {
                    throw new InventoryServiceException("Inventory service is unavailable");
                })
                .body(CustomIngredientSnapshot.class);
    }

    @Override
    public void reserve(Map<UUID, Double> items) {
        restClient.post()
                .uri("/api/inventory/ingredients/reserve")
                .body(new ReservationRequest(items))
                .retrieve()
                .onStatus(HttpStatusCode::isError, (request, response) -> {
                    throw new InventoryServiceException("Failed to reserve inventory");
                })
                .toBodilessEntity();
    }

    @Override
    public void commit(Map<UUID, Double> items) {
        restClient.post()
                .uri("/api/inventory/ingredients/commit")
                .body(new ReservationRequest(items))
                .retrieve()
                .onStatus(HttpStatusCode::isError, (request, response) -> {
                    throw new InventoryServiceException("Failed to commit inventory");
                })
                .toBodilessEntity();
    }

    @Override
    public void rollback(Map<UUID, Double> items) {
        restClient.post()
                .uri("/api/inventory/ingredients/rollback")
                .body(new ReservationRequest(items))
                .retrieve()
                .onStatus(HttpStatusCode::isError, (request, response) -> {
                    throw new InventoryServiceException("Failed to rollback inventory");
                })
                .toBodilessEntity();
    }

    private record ReservationRequest(Map<UUID, Double> items) {}
}
