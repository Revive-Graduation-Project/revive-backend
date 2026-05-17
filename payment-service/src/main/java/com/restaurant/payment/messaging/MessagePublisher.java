package com.restaurant.payment.messaging;

import com.restaurant.payment.dto.PaymentIntentCreatedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class MessagePublisher {

    private final RabbitTemplate rabbitTemplate;

    @Value("${app.rabbitmq.exchange}")
    private String exchange;

    @Value("${app.rabbitmq.outbound.payment-succeeded}")
    private String paymentSucceededRoutingKey;

    @Value("${app.rabbitmq.outbound.payment-failed}")
    private String paymentFailedRoutingKey;

    public void publishPaymentSucceeded(com.restaurant.payment.dto.PaymentSucceededEvent event) {
        log.info("Publishing PaymentSucceededEvent for order: {}", event.getOrderId());
        rabbitTemplate.convertAndSend(exchange, paymentSucceededRoutingKey, event);
    }

    public void publishPaymentFailed(com.restaurant.payment.dto.PaymentFailedEvent event) {
        log.info("Publishing PaymentFailedEvent for order: {}", event.getOrderId());
        rabbitTemplate.convertAndSend(exchange, paymentFailedRoutingKey, event);
    }
}
