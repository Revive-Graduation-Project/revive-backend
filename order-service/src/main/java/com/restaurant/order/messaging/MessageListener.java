package com.restaurant.order.messaging;

import com.restaurant.order.events.TicketCreatedEvent;
import com.restaurant.order.events.TicketCreationFailedEvent;
import com.restaurant.order.events.TicketReadyEvent;
import com.restaurant.order.service.OrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class MessageListener {

    private final OrderService orderService;

    @RabbitListener(queues = "${app.rabbitmq.queues.ticket-created.name}")
    public void onTicketCreated(TicketCreatedEvent event) {
        log.info("Received ticket.created event for orderId: {}, ticketId: {}", event.getId(), event.getTicketId());
        orderService.confirmOrder(event.getId(), event.getTicketId());
    }

    @RabbitListener(queues = "${app.rabbitmq.queues.ticket-failed.name}")
    public void onTicketCreationFailed(TicketCreationFailedEvent event) {
        log.info("Received ticket.failed event for orderId: {}, reason: {}", event.getId(), event.getReason());
        orderService.cancelOrder(event.getId(), event.getReason());
    }

    @RabbitListener(queues = "ticket.ready.queue")
    public void onTicketReady(TicketReadyEvent event) {
        log.info("Received ticket.ready event for orderId: {}", event.getId());
        orderService.onTicketReady(event.getId());
    }
}
