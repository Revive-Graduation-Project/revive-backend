package com.restaurant.order.messaging;

import com.restaurant.order.events.OrderCreatedEvent;
import com.restaurant.order.events.payments.PaymentRefundRequestedEvent;
import com.restaurant.order.events.payments.PaymentRequestedEvent;
import com.restaurant.order.events.points.PointRedemptionRollbackRequestedEvent;
import com.restaurant.order.events.points.PointRedemptionRequestedEvent;
import com.restaurant.order.events.points.RewardPointsEarnedEvent;
import jakarta.annotation.Nullable;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.UUID;

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

    public void publishPaymentRequested(PaymentRequestedEvent event, String sagaId, String correlationId) {
        send("payment.requested", event, sagaId, correlationId);
        log.info("Payment requested event published for orderId: {}", event.getOrderId());
    }

    public void publishPointRedemptionRequested(PointRedemptionRequestedEvent event, String sagaId, String correlationId) {
        send("points.redemption.requested", event, sagaId, correlationId);
        log.info("Point redemption requested event published for orderId: {}", event.getOrderId());
    }

    public void publishRewardPointsEarned(RewardPointsEarnedEvent event) {
        // Simple fire and forget for rewarding
        send("points.earned", event, null, UUID.randomUUID().toString());
        log.info("Reward points earned event published for orderId: {}, points: {}", event.getOrderId(), event.getPointsEarned());
    }

    public void publishPointRedemptionRollback(PointRedemptionRollbackRequestedEvent event, String sagaId, String correlationId) {
        send("points.redemption.rollback", event, sagaId, correlationId);
        log.info("Point redemption rollback event published for orderId: {}", event.getOrderId());
    }

    public void publishPaymentRefund(PaymentRefundRequestedEvent event, String sagaId, String correlationId) {
        send("payment.refund.requested", event, sagaId, correlationId);
        log.info("Payment refund requested event published for orderId: {}", event.getOrderId());
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
