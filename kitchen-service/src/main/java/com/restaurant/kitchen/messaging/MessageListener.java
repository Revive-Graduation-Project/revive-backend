package com.restaurant.kitchen.messaging;

import com.restaurant.kitchen.events.ProfileCreationFailedEvent;
import com.restaurant.kitchen.events.UserCreatedEvent;
import com.restaurant.kitchen.mapper.ChefProfileMapper;
import com.restaurant.kitchen.service.KitchenService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;


@Component
@RequiredArgsConstructor
@Slf4j
public class MessageListener {

    private final KitchenService service;
    private final ChefProfileMapper chefMapper;
    private final MessagePublisher publisher;

    @RabbitListener(queues = "${app.rabbitmq.queues.user-created.name}")
    public void onUserCreated(UserCreatedEvent event,
                              @Header(value = "correlationId", required = false) String correlationId,
                              @Header(value = "sagaId", required = false) String sagaId) {

        if (sagaId == null || correlationId == null) {
            log.error("Missing required headers, discarding message, event: {}", event);
            return;  // discard — No requeue
        }
        service.createChefProfile(event, correlationId, sagaId);
    }

    @RabbitListener(queues = "${app.rabbitmq.queues.user-created.dlq-name}")
    public void onUserCreatedFailure(UserCreatedEvent event,
                                     @Header("sagaId") String sagaId,
                                     @Header("correlationId") String correlationId) {

        log.error("Message moved to DLQ after max retries. Sending final failure event for user: {}", event.getAuthUserId());

        try {

            ProfileCreationFailedEvent failedEvent = chefMapper.toProfileCreationFailedEvent(event);
            failedEvent.setReason("Chef profile creation failed due to technical error after multiple retries");
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
}
