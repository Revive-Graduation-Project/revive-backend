package com.restaurant.inventory.messaging;

import com.restaurant.inventory.event.MenuNutritionEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@RequiredArgsConstructor
public class MenuNutritionPublisher {

    private final RabbitTemplate rabbitTemplate;

    @Value("${app.rabbitmq.exchange}")
    private String exchange;

    @Value("${app.rabbitmq.queues.menu-nutrition.routing-key}")
    private String routingKey;

    public void publish(MenuNutritionEvent event) {
        try {
            rabbitTemplate.convertAndSend(exchange, routingKey, event);
            log.info("Published menu-nutrition event with {} meals to exchange '{}' with routing key '{}'",
                    event.meals().size(), exchange, routingKey);
        } catch (Exception e) {
            log.error("Failed to publish menu-nutrition event: {}", e.getMessage(), e);
            throw e;
        }
    }
}
