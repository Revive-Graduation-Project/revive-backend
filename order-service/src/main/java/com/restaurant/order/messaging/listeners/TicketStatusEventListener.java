package com.restaurant.order.messaging.listeners;

import com.restaurant.order.enums.OrderStatus;
import com.restaurant.order.enums.TicketStatus;
import com.restaurant.order.events.TicketStatusUpdatedEvent;
import com.restaurant.order.service.OrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class TicketStatusEventListener {

    private final OrderService orderService;

    @RabbitListener(queues = "${app.rabbitmq.queues.ticket-status-updated.name}")
    public void handleTicketStatusUpdated(TicketStatusUpdatedEvent event) {
        log.info("Received ticket status update event: {}", event);

        Long orderId = event.getOrderId();
        TicketStatus status = event.getStatus();

        if (status == null) {
            log.warn("Received ticket status update with null status for orderId: {}", orderId);
            return;
        }

        try {
            switch (status) {
                case PREPARING -> orderService.onTicketStarted(orderId, event.getTicketId());
                case READY -> orderService.onTicketReady(orderId);
                case CANCELED -> orderService.processTicketCancellationSuccess(orderId);
                // DONE might just map to READY or stay READY in OrderService?
                case DONE -> orderService.onTicketDone(orderId);
                default -> log.warn("Received unmapped ticket status '{}' for orderId: {}", status, orderId);
            }
        } catch (Exception e) {
            log.error("Failed to process ticket status update event for orderId: {}", orderId, e);
            throw e; // Rely on RabbitMQ retry if configured
        }
    }
}
