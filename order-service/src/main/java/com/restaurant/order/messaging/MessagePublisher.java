package com.restaurant.order.messaging;

import com.restaurant.order.events.OrderCreatedEvent;
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

    public void publishOrderCreated(OrderCreatedEvent event, String sagaId, String correlationId) {
        send("order.created", event, sagaId, correlationId);
        log.info("Order created event published for orderId: {}", event.getId());
    }

    private void send(String routingKey, Object payload, @Nullable String sagaId, String correlationId) {
        try {
            rabbitTemplate.convertAndSend(exchange, routingKey, payload, message -> {
                if (sagaId != null)
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
