package com.restaurant.order.client;

import com.restaurant.order.dto.snapshot.MealSnapshot;
import com.restaurant.order.exception.MenuServiceException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.UUID;

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
    public MealSnapshot getMealById(UUID mealId) {
        log.info("Fetching meal snapshot from menu-service for mealId: {}", mealId);

        return restClient.get()
                .uri("/api/menu/meals/{mealId}", mealId)
                .retrieve()
                .onStatus(HttpStatusCode::is4xxClientError, (request, response) -> {
                    throw new MenuServiceException("Meal not found with ID: " + mealId);
                })
                .onStatus(HttpStatusCode::is5xxServerError, (request, response) -> {
                    throw new MenuServiceException("Menu service is unavailable");
                })
                .body(MealSnapshot.class);
    }
}
