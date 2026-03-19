package com.restaurant.kitchen.messaging;

import com.restaurant.kitchen.events.ProfileCreatedEvent;
import com.restaurant.kitchen.events.ProfileCreationFailedEvent;
import com.restaurant.kitchen.events.TicketReadyEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;


@Component
@Slf4j
@RequiredArgsConstructor
public class MessagePublisher {

    private final RabbitTemplate rabbitTemplate;

    @Value("${app.rabbitmq.exchange}")
    private String exchange;

    public void publishChefCreated(ProfileCreatedEvent event, String sagaId, String correlationId) {
        send("chef-profile.created", event, sagaId, correlationId);
    }

    public void publishChefFailed(ProfileCreationFailedEvent event, String sagaId, String correlationId) {
        send("chef-profile.failed", event, sagaId, correlationId);
    }

    public void publishTicketReady(Long ticketId, TicketReadyEvent event, String sagaId, String correlationId) {
        send("ticket.ready", event, sagaId, correlationId);
        log.info("Event published for ticket {}", ticketId);
    }

    private void send(String routingKey, Object payload, String sagaId, String correlationId) {
        try {
            rabbitTemplate.convertAndSend(exchange, routingKey, payload, message -> {
                message.getMessageProperties().setHeader("sagaId", sagaId);
                message.getMessageProperties().setHeader("correlationId", correlationId);
                return message;
            });
        } catch (Exception e) {
            log.error("Messaging error for routing key {}: {}", routingKey, e.getMessage());
            throw e;
        }
    }
}
