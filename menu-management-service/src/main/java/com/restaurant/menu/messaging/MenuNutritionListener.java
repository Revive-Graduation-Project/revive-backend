package com.restaurant.menu.messaging;

import com.restaurant.menu.event.MenuNutritionEvent;
import com.restaurant.menu.service.MealService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@RequiredArgsConstructor
public class MenuNutritionListener {

    private final MealService mealService;

    @RabbitListener(queues = "${app.rabbitmq.queues.menu-nutrition.name}")
    public void handleMenuNutritionEvent(MenuNutritionEvent event) {
        log.info("Received menu-nutrition event with {} meals", event.meals().size());
        mealService.processNutritionEvent(event);
    }
}
