package com.restaurant.kitchen.service.impl;

import com.restaurant.kitchen.entity.ChefProfile;
import com.restaurant.kitchen.enums.ChefStatus;
import com.restaurant.kitchen.enums.Station;
import com.restaurant.kitchen.events.UserCreatedEvent;
import com.restaurant.kitchen.events.chefProfileEvents.ProfileCreatedEvent;
import com.restaurant.kitchen.exception.ChefNotFoundException;
import com.restaurant.kitchen.messaging.MessagePublisher;
import com.restaurant.kitchen.dto.ChefProfileDTO;
import com.restaurant.kitchen.mapper.ChefProfileMapper;
import com.restaurant.kitchen.repository.ChefProfileRepository;
import com.restaurant.kitchen.service.ChefService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(rollbackFor = Exception.class)
public class ChefServiceImpl implements ChefService {

    private final MessagePublisher publisher;
    private final ChefProfileRepository chefProfileRepository;
    private final ChefProfileMapper chefProfileMapper;


    @Override
    public void createChefProfile(UserCreatedEvent event, String correlationId, String sagaId) {

        log.info("Received user.created event: {} , sagaID: {} , correlationID {}", event, sagaId, correlationId);

        if (!"CHEF".equals(event.getRole())) {
            log.info("Ignoring non-chef user: {}", event.getId());
            return;  //spring Ack the message automatically since it did not throw an exception
        }

        Optional<ChefProfile> existingChef =
                chefProfileRepository.findByAuthUserId(event.getId());

        // Idempotency check
        if (existingChef.isPresent()) {

            log.info("Chef already exists, re-publishing creation event. userId: {}", event.getId());

            publisher.publishChefCreated(
                    new ProfileCreatedEvent(event.getId()),
                    sagaId, correlationId);

            return; //spring Ack the message automatically since it did not throw an exception
        }

        try {
            // Temporary data until the admin completes their information with endpoints.
              chefProfileRepository.save(
                     ChefProfile.builder()
                             .authUserId(event.getId())
                             .station(Station.UNASSIGNED)
                             .displayName("Unassigned")
                             .status(ChefStatus.INACTIVE)
                             .build()
            );
            publisher.publishChefCreated(
                    new ProfileCreatedEvent(event.getId()),
                    sagaId, correlationId);

        } catch (Exception exception) {
            log.error("Chef profile creation failed", exception); // due to publish failure or save failure

            // rollback DB if save succeeded but publish failed → prevents duplicate on retry
            // and nack the message to re-queue or route to dlq if max-attempts have been reached
            // when goes to dlq then publish user-created failed event to tell the engaged services to roll back
            throw new RuntimeException("Chef profile creation failed for userId: " + event.getId());
        }
    }

    private ChefProfile findChef(Long id) {

        return chefProfileRepository.findById(id)
                .orElseThrow(() -> new ChefNotFoundException(id));

    }

    @Override
    public ChefProfileDTO updateChefDisplayName(Long id, String displayName) {

        ChefProfile retrievedChef = findChef(id);
        retrievedChef.setDisplayName(displayName.trim());
        // no need to re-save cause transactional triggers dirty checking
        // which auto updates DB entries automatically when object state change is detected
        return chefProfileMapper.toDTO(retrievedChef);
    }

    @Override
    public ChefProfileDTO updateChefStation(Long id, Station station) {

        ChefProfile retrievedChef = findChef(id);
        retrievedChef.setStation(station);
        return chefProfileMapper.toDTO(retrievedChef);
    }

    @Override
    public ChefProfileDTO updateChefStatus(Long id, ChefStatus status) {

        ChefProfile retrievedChef = findChef(id);
        retrievedChef.setStatus(status);
        return chefProfileMapper.toDTO(retrievedChef);
    }
}
