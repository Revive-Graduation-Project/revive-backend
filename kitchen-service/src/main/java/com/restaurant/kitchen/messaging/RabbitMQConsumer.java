package com.restaurant.kitchen.messaging;

import com.restaurant.kitchen.events.UserCreatedEvent;
import com.restaurant.kitchen.service.KitchenService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;


@Component
@RequiredArgsConstructor
@Slf4j
public class RabbitMQConsumer {

    private final MessageHandler messageHandler;

    @RabbitListener(queues = "${app.rabbitmq.queues.user-created.name}")
    public void handleUserCreated(UserCreatedEvent event, @Header(value = "correlationId", required = false) String correlationId,
                                  @Header(value = "sagaId", required = false) String sagaId) {
        if (sagaId == null || correlationId == null) {
            log.error("Missing required headers, discarding message, event: {}", event);
            return;  // discard — No requeue
        }
        messageHandler.createChefProfile(event, correlationId, sagaId);
    }
}
