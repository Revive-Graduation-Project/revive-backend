package com.restaurant.client.event;

import com.restaurant.client.domain.entity.ClientProfile;
import com.restaurant.client.domain.enums.Gender;
import com.restaurant.client.domain.enums.Goal;
import com.restaurant.client.domain.enums.HealthCondition;
import com.restaurant.client.service.ClientProfileService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Set;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
@Slf4j
public class UserCreatedEventListener {

    private final ClientProfileService clientProfileService;
    private final RabbitTemplate rabbitTemplate;

    @Value("${app.rabbitmq.exchange}")
    private String exchange;

    @Value("${app.rabbitmq.queues.auth-compensation.routing-key}")
    private String compensationRoutingKey;

    @RabbitListener(queues = "${app.rabbitmq.queues.client-user-created.name}")
    public void handleUserCreatedEvent(UserCreatedEvent event) {
        log.info("Received UserCreatedEvent for user ID: {}", event.getId());

        // We only care about CLIENT roles
        if (!"CLIENT".equals(event.getRole())) {
            log.info("Ignoring event for non-client role: {}", event.getRole());
            return;
        }

        try {
            clientProfileService.createProfileFromEvent(event);
        } catch (Exception e) {
            log.error("Failed to process UserCreatedEvent for user ID: {}. Initiating SAGA compensation...", event.getId(), e);

            // Publish Compensation Event
            ProfileCreationFailedEvent compensationEvent = ProfileCreationFailedEvent.builder()
                    .id(event.getId())
                    .build();

            rabbitTemplate.convertAndSend(exchange, compensationRoutingKey, compensationEvent);
            log.info("Published compensation event for user ID: {}", event.getId());
        }
    }
}
