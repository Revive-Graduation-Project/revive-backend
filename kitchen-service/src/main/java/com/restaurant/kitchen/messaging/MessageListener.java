package com.restaurant.kitchen.messaging;

import com.restaurant.kitchen.events.OrderCanceledEvent;
import com.restaurant.kitchen.events.OrderCreatedEvent;
import com.restaurant.kitchen.events.chefProfileEvents.ProfileCreationFailedEvent;
import com.restaurant.kitchen.events.UserCreatedEvent;
import com.restaurant.kitchen.events.ticketEvents.TicketCanceledFailedEvent;
import com.restaurant.kitchen.events.ticketEvents.TicketCreationFailedEvent;
import com.restaurant.kitchen.service.ChefService;
import com.restaurant.kitchen.service.TicketService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;


@Component
@RequiredArgsConstructor
@Slf4j
public class MessageListener {

    private final ChefService chefService;
    private final TicketService ticketService;

    private final MessagePublisher publisher;

    @RabbitListener(queues = "${app.rabbitmq.queues.user-created.name}")
    public void onUserCreated(UserCreatedEvent event,
                              @Header(value = "correlationId", required = false) String correlationId,
                              @Header(value = "sagaId", required = false) String sagaId) {

        chefService.createChefProfile(event, correlationId, sagaId);
    }

    @RabbitListener(queues = "${app.rabbitmq.queues.user-created.dlq-name}")
    public void onUserCreatedFailure(UserCreatedEvent event,
                                     @Header("sagaId") String sagaId,
                                     @Header("correlationId") String correlationId) {

        log.error("Message moved to DLQ after max retries. Sending final failure event for user: {}", event.getId());

        try {

            ProfileCreationFailedEvent failedEvent = new ProfileCreationFailedEvent(
                    event.getId(),
                    "Chef profile creation failed due to technical error after multiple retries"
            );
            publisher.publishChefFailed(failedEvent, sagaId, correlationId);

        } catch (Exception e) {

            // handle failed event publish failure
            log.error("CRITICAL: DLQ Processing failed for Saga: {}. " +
                            "Manual intervention required! Event Data: {}",
                    sagaId, event, e);

            // By not throwing an exception here, we effectively ACK the message.
            // It leaves the queue (preventing a loop), but the data is safe in LOGS.
        }
    }


    @RabbitListener(queues = "${app.rabbitmq.queues.order-created.name}")
    public void onOrderCreated(OrderCreatedEvent event,
                               @Header(value = "correlationId", required = false) String correlationId,
                               @Header(value = "sagaId", required = false) String sagaId) {

        ticketService.createKitchenTicket(event, correlationId, sagaId);
    }

    @RabbitListener(queues = "${app.rabbitmq.queues.order-created.dlq-name}")
    public void onOrderCreatedFailure(OrderCreatedEvent event,
                                     @Header("sagaId") String sagaId,
                                     @Header("correlationId") String correlationId) {

        log.error("Message moved to DLQ after max retries. Sending final failure event for order: {}", event.getId());

        try {

            TicketCreationFailedEvent failedEvent = new TicketCreationFailedEvent(
                    event.getId(),
                    "Kitchen ticket creation failed due to technical error after multiple retries"
            );
            publisher.publishTicketFailed(failedEvent, sagaId, correlationId);

        } catch (Exception e) {

            // handle failed event publish failure
            log.error("CRITICAL: DLQ Processing failed for Saga: {}. " +
                            "Manual intervention required! Event Data: {}",
                    sagaId, event, e);

            // By not throwing an exception here, we effectively ACK the message.
            // It leaves the queue (preventing a loop), but the data is safe in LOGS.
        }
    }
    @RabbitListener(queues = "${app.rabbitmq.queues.order-canceled.name}")
    public void onOrderCancelled(OrderCanceledEvent event,
                                 @Header(value = "sagaId" , required = false) String sagaId,
                                 @Header(value = "correlationId" , required = false) String correlationId) {

        ticketService.cancelKitchenTicket(event.getId(), sagaId, correlationId);
    }

    @RabbitListener(queues = "${app.rabbitmq.queues.order-canceled.dlq-name}")
    public void onOrderCancelledFailure(OrderCanceledEvent event,
                                        @Header("sagaId") String sagaId,
                                        @Header("correlationId") String correlationId) {

        log.error("Message moved to DLQ after max retries. Sending final failure event for order: {}", event.getId());

        try {

            TicketCanceledFailedEvent failedEvent = new TicketCanceledFailedEvent(
                    event.getId(),
                    "Kitchen ticket creation failed due to technical error after multiple retries"
            );
            publisher.publishTicketCancellationFailure(failedEvent, sagaId, correlationId);

        } catch (Exception e) {

            // handle failed event publish failure
            log.error("CRITICAL: DLQ Processing failed for Saga: {}. " +
                            "Manual intervention required! Event Data: {}",
                    sagaId, event, e);

        }
    }
}
