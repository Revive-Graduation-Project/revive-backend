package com.restaurant.client.event;

import com.restaurant.client.service.ClientProfileService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class PointsEventListener {

    private final ClientProfileService clientProfileService;
    private final RabbitTemplate rabbitTemplate;

    @Value("${app.rabbitmq.exchange}")
    private String exchange;

    @Value("${app.rabbitmq.queues.points-redemption-succeeded.routing-key}")
    private String successRoutingKey;

    @Value("${app.rabbitmq.queues.points-redemption-failed.routing-key}")
    private String failureRoutingKey;

    @RabbitListener(queues = "${app.rabbitmq.queues.points-redemption-requested.name}")
    public void handlePointRedemptionRequested(PointRedemptionRequestedEvent event) {
        log.info("Received PointRedemptionRequestedEvent for client: {}, order: {}, points: {}", 
                event.getClientId(), event.getOrderId(), event.getPointsToRedeem());

        try {
            clientProfileService.redeemPoints(event.getClientId(), event.getPointsToRedeem(), event.getOrderId());
            
            PointRedemptionSucceededEvent successEvent = PointRedemptionSucceededEvent.builder()
                    .clientId(event.getClientId())
                    .orderId(event.getOrderId())
                    .build();
            
            rabbitTemplate.convertAndSend(exchange, successRoutingKey, successEvent);
            log.info("Published PointRedemptionSucceededEvent for order: {}", event.getOrderId());
            
        } catch (Exception e) {
            log.error("Failed to redeem points for client: {}, order: {}. Error: {}", 
                    event.getClientId(), event.getOrderId(), e.getMessage());
            
            PointRedemptionFailedEvent failureEvent = PointRedemptionFailedEvent.builder()
                    .clientId(event.getClientId())
                    .orderId(event.getOrderId())
                    .reason(e.getMessage())
                    .build();
            
            rabbitTemplate.convertAndSend(exchange, failureRoutingKey, failureEvent);
            log.info("Published PointRedemptionFailedEvent for order: {}", event.getOrderId());
        }
    }

    @RabbitListener(queues = "${app.rabbitmq.queues.points-earned.name}")
    public void handlePointsEarned(PointsEarnedEvent event) {
        log.info("Received PointsEarnedEvent for client: {}, order: {}, points: {}", 
                event.getClientId(), event.getOrderId(), event.getPointsEarned());

        try {
            clientProfileService.addPoints(event.getClientId(), event.getPointsEarned(), event.getOrderId());
            log.info("Successfully added points for client: {}", event.getClientId());
        } catch (Exception e) {
            log.error("Failed to add points for client: {}. Error: {}", event.getClientId(), e.getMessage());
            // Usually earning points doesn't have a failure event in the same way as redemption
        }
    }
}
