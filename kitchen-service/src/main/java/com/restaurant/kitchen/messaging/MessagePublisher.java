package com.restaurant.kitchen.messaging;

import com.restaurant.kitchen.events.chefProfileEvents.ProfileCreatedEvent;
import com.restaurant.kitchen.events.chefProfileEvents.ProfileCreationFailedEvent;
import com.restaurant.kitchen.events.ticketEvents.*;
import jakarta.annotation.Nullable;
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
        send("chef.profile.created", event, sagaId, correlationId);
    }

    public void publishChefFailed(ProfileCreationFailedEvent event, String sagaId, String correlationId) {
        send("chef.profile.failed", event, sagaId, correlationId);
    }

    public void publishTicketCreated(TicketCreatedEvent event, String sagaId, String correlationId) {
        send("ticket.created", event, sagaId, correlationId);
        log.info("Ticket created event published for order {} . ticket id: {}", event.getId() , event.getTicketId());
    }

    public void publishTicketFailed(TicketCreationFailedEvent event, String sagaId, String correlationId) {
        send("ticket.failed", event, sagaId, correlationId);
        log.info("Ticket creation failed Event published for order {}", event.getId());
    }

    public void publishTicketReady(TicketReadyEvent event, String correlationId) {
        send("ticket.ready", event, null, correlationId); // simple point-to-point communication no saga needed
        log.info("Ticket ready event published for order {} . ticket id: {}", event.getId() , event.getTicketId());
    }

    public void publishTicketStarted(TicketStartedEvent event, String correlationId) {
        send("ticket.started", event, null, correlationId); // simple point-to-point communication no saga needed
        log.info("Ticket started event published for order {} . ticket id: {}", event.getId() , event.getTicketId());
    }

    public void publishTicketCanceled(TicketCanceledEvent event, String correlationId) {
        send("ticket.canceled", event, null, correlationId); // simple point-to-point communication no saga needed
        log.info("Ticket canceled event published for order {} . ticket id: {}", event.getId() , event.getTicketId());
    }
    public void publishTicketCancellationFailure(TicketCanceledFailedEvent event, String sagaId, String correlationId) {
        send("ticket.canceled.failed", event, sagaId, correlationId);
        log.info("Ticket canceled failed event published for order {}", event.getId());
    }

    private void send(String routingKey, Object payload, @Nullable String sagaId, String correlationId) {
        try {
            rabbitTemplate.convertAndSend(exchange, routingKey, payload, message -> {
               if(sagaId != null)
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
