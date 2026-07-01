package com.restaurant.auth.messaging;

import com.restaurant.auth.event.ProfileCreationFailedEvent;
import com.restaurant.auth.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.restaurant.auth.config.RabbitMQConfig;

@Slf4j
@Component
@RequiredArgsConstructor
public class SagaCompensationListener {

    private final UserRepository userRepository;

    /**
     * Saga Compensation Handler.
     *
     * <p>
     * Triggered when the downstream service fails to create a chef profile
     * (routing key: "chef.profile.failed"). Performs a hard-delete of the
     * partially-created user to rollback the distributed transaction.
     * </p>
     */
    @Transactional
    @RabbitListener(queues = {RabbitMQConfig.COMPENSATION_QUEUE, RabbitMQConfig.CLIENT_COMPENSATION_QUEUE})
    public void handleProfileCreationFailed(ProfileCreationFailedEvent event) {
        log.warn("Saga compensation triggered for userId={}", event.getId());

        var userOpt = userRepository.findById(event.getId());

        if (userOpt.isEmpty()) {
            log.warn("Saga compensation ignored: User id={} already deleted or not found", event.getId());
            return;
        }

        userRepository.delete(userOpt.get());
        log.warn("Hard-deleted user id={} as saga compensation", event.getId());
    }
}
