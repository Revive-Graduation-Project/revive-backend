package com.restaurant.order.messaging;

import com.restaurant.order.events.payments.PaymentFailedEvent;
import com.restaurant.order.events.payments.PaymentSucceededEvent;
import com.restaurant.order.events.points.PointRedemptionFailedEvent;
import com.restaurant.order.events.points.PointRedemptionSucceededEvent;
import com.restaurant.order.events.tickets.TicketCreatedEvent;
import com.restaurant.order.events.tickets.TicketCreationFailedEvent;
import com.restaurant.order.events.tickets.TicketReadyEvent;
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

    @RabbitListener(queues = "${app.rabbitmq.queues.ticket-ready.name}")
    public void onTicketReady(TicketReadyEvent event) {
        log.info("Received ticket.ready event for orderId: {}", event.getId());
        orderService.onTicketReady(event.getId());
    }

    @RabbitListener(queues = "${app.rabbitmq.queues.payment-succeeded.name}")
    public void onPaymentSucceeded(PaymentSucceededEvent event) {
        log.info("Received payment.succeeded event for orderId: {}", event.getOrderId());
        orderService.markPaymentSucceeded(event.getOrderId());
    }

    @RabbitListener(queues = "${app.rabbitmq.queues.payment-failed.name}")
    public void onPaymentFailed(PaymentFailedEvent event) {
        log.info("Received payment.failed event for orderId: {}, reason: {}", event.getOrderId(), event.getReason());
        orderService.markPaymentFailed(event.getOrderId(), event.getReason());
    }

    @RabbitListener(queues = "${app.rabbitmq.queues.point-redemption-succeeded.name}")
    public void onPointRedemptionSucceeded(PointRedemptionSucceededEvent event) {
        log.info("Received point-redemption.succeeded event for orderId: {}", event.getOrderId());
        orderService.markPointRedemptionSucceeded(event.getOrderId());
    }

    @RabbitListener(queues = "${app.rabbitmq.queues.point-redemption-failed.name}")
    public void onPointRedemptionFailed(PointRedemptionFailedEvent event) {
        log.info("Received point-redemption.failed event for orderId: {}, reason: {}", event.getOrderId(), event.getReason());
        orderService.markPointRedemptionFailed(event.getOrderId(), event.getReason());
    }
}
