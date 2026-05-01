package com.restaurant.auth.messaging;

import com.restaurant.auth.event.ProfileCreationFailedEvent;
import com.restaurant.auth.exception.UserNotFoundException;
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
    @RabbitListener(queues = RabbitMQConfig.COMPENSATION_QUEUE)
    public void handleProfileCreationFailed(ProfileCreationFailedEvent event) {
        log.warn("Saga compensation triggered for userId={}", event.getId());

        var user = userRepository.findById(event.getId())
                .orElseThrow(() -> new UserNotFoundException(event.getId()));

        userRepository.delete(user);
        log.warn("Hard-deleted user id={} as saga compensation", event.getId());
    }
}
