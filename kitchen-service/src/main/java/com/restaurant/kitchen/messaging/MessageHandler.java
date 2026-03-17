package com.restaurant.kitchen.messaging;

import com.restaurant.kitchen.entity.ChefProfile;
import com.restaurant.kitchen.entity.KitchenTicket;
import com.restaurant.kitchen.events.ProfileCreationFailedEvent;
import com.restaurant.kitchen.events.UserCreatedEvent;
import com.restaurant.kitchen.mapper.ChefProfileMapper;
import com.restaurant.kitchen.mapper.KitchenTicketMapper;
import com.restaurant.kitchen.repository.ChefProfileRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Component
@Slf4j
@RequiredArgsConstructor
public class MessageHandler {

    private final RabbitTemplate rabbitTemplate;
    private final ChefProfileRepository chefProfileRepository;
    private final ChefProfileMapper chefMapper;
    private final KitchenTicketMapper ticketMapper;

    @Value("${app.rabbitmq.exchange}")
    private String exchange;

    public void createChefProfile(UserCreatedEvent event, String correlationId, String sagaId) {

        log.info("Received userCreated event: {} , sagaID: {} , correlationID {}", event, sagaId, correlationId);

        if (!"CHEF".equals(event.getRole())) {
            log.info("Ignoring non-chef user: {}", event.getAuthUserId());
            return;
        }

        Map<String, String> headers = Map.of(
                "sagaId", sagaId,
                "correlationId", correlationId
        );

        Optional<ChefProfile> existingChef =
                chefProfileRepository.findByAuthUserId(event.getAuthUserId());

        if (existingChef.isPresent()) {

            log.info("Chef already exists, re-publishing creation event. userId: {}", event.getAuthUserId());

            publishChefCreatedEvent(existingChef.get(), headers);

            return; //spring Ack the message automatically since it did not throw an exception
        }

        try {

            ChefProfile chef = chefProfileRepository.save(chefMapper.toEntity(event));

            publishChefCreatedEvent(chef, headers);

            log.info("Chef profile created successfully. userId: {}", event.getAuthUserId());

        } catch (Exception saveException) {

            log.error("Chef profile creation failed. event: {}", event, saveException);

            publishChefCreationFailedEvent(event, saveException.getMessage(), headers);
        }
    }

    private void publishChefCreatedEvent(ChefProfile chef, Map<String, String> headers) {

        try {

            rabbitTemplate.convertAndSend(
                    exchange,
                    "chef-profile.created",
                    chefMapper.toProfileCreatedEvent(chef),
                    message -> {
                        message.getMessageProperties().getHeaders().putAll(headers);
                        return message;
                    });

        } catch (Exception publishException) {

            log.error("Failed to publish chef-profile.created event", publishException);

            throw publishException; //message process failed so spring nack and re-queue it or push it to dlq
        }
    }

    private void publishChefCreationFailedEvent(UserCreatedEvent event,
                                                String reason,
                                                Map<String, String> headers) {

        ProfileCreationFailedEvent failedEvent =
                chefMapper.toProfileCreationFailedEvent(event);

        failedEvent.setReason(reason);

        try {

            rabbitTemplate.convertAndSend(
                    exchange,
                    "chef-profile.failed",
                    failedEvent,
                    message -> {
                        message.getMessageProperties().getHeaders().putAll(headers);
                        return message;
                    });

        } catch (Exception publishException) {

            log.error("Failed to publish chef-profile.failed event", publishException);

            throw publishException; //message process failed so spring nack and re-queue it or push it to dlq
        }
    }

    public void publishTicketReadyEvent(Long id, KitchenTicket ticket) {

        Map<String, String> headers = Map.of(
                "sagaId", UUID.randomUUID().toString(),
                "correlationId", UUID.randomUUID().toString()
        );

        try {
            rabbitTemplate.convertAndSend(exchange, "ticket.ready",
                    ticketMapper.toTicketReadyEvent(ticket),
                    message -> {
                        message.getMessageProperties().getHeaders().putAll(headers);
                        return message;
                    }
            );
            log.info("Event published for ticket {}", id);
        } catch (Exception e) {
            log.error("Failed to publish event for ticket {}", id, e);
            throw new RuntimeException(); // triggers @Transactional rollback. global handler will catch it
        }
    }
}
