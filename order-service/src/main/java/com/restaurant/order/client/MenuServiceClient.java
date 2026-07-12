package com.restaurant.order.client;

import com.restaurant.order.dto.request.OrderItemRequest;
import com.restaurant.order.dto.snapshot.MealPriceSnapshot;
import com.restaurant.order.exception.MenuServiceException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.List;

@Component
@Slf4j
public class MenuServiceClient implements MenuClient {

    private final RestClient restClient;

    public MenuServiceClient(RestClient.Builder restClientBuilder,
                             @Value("${app.services.menu-url}") String menuBaseUrl) {
        this.restClient = restClientBuilder
                .baseUrl(menuBaseUrl)
                .build();
    }

    @Override
    public MealPriceSnapshot getMealById(Long id) {
        log.info("Fetching meal price from menu-service for mealId: {}", id);

        return restClient.get()
                .uri("/api/menu/{id}", id)
                .retrieve()
                .onStatus(HttpStatusCode::is4xxClientError, (request, response) -> {
                    throw new MenuServiceException("Meal not found with ID: " + id);
                })
                .onStatus(HttpStatusCode::is5xxServerError, (request, response) -> {
                    throw new MenuServiceException("Menu service is unavailable");
                })
                .body(MealPriceSnapshot.class);
    }

    @Override
    public List<com.restaurant.order.dto.IngredientDTO> getAllIngredients() {
        log.info("Fetching all ingredients from menu-service for price calculation");
        return restClient.get()
                .uri("/api/ingredients")
                .retrieve()
                .onStatus(HttpStatusCode::isError, (request, response) -> {
                    throw new MenuServiceException("Failed to fetch ingredients from menu service");
                })
                .body(new org.springframework.core.ParameterizedTypeReference<List<com.restaurant.order.dto.IngredientDTO>>() {});
    }

    @Override
    public void reserveStock(List<OrderItemRequest> items) {
        log.info("Reserving stock in menu-service for {} items...", items.size());
        restClient.post()
                .uri("/api/ingredients/reserve")
                .body(items)
                .retrieve()
                .onStatus(HttpStatusCode::isError, (req, res) -> {
                    throw new MenuServiceException("Failed to reserve stock in menu service");
                })
                .toBodilessEntity();
    }

    @Override
    public void rollbackStock(List<OrderItemRequest> items) {
        log.info("Rolling back stock in menu-service for {} items...", items.size());
        restClient.post()
                .uri("/api/ingredients/revert")
                .body(items)
                .retrieve()
                .onStatus(HttpStatusCode::isError, (req, res) -> {
                    throw new MenuServiceException("Failed to rollback stock in menu service");
                })
                .toBodilessEntity();
    }
}
