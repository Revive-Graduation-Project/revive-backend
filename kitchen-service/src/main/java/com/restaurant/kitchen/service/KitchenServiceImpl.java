package com.restaurant.kitchen.service;

import com.restaurant.kitchen.dto.KitchenTicketDTO;
import com.restaurant.kitchen.entity.ChefProfile;
import com.restaurant.kitchen.entity.KitchenTicket;
import com.restaurant.kitchen.enums.ChefStatus;
import com.restaurant.kitchen.enums.Station;
import com.restaurant.kitchen.enums.TicketStatus;
import com.restaurant.kitchen.events.UserCreatedEvent;
import com.restaurant.kitchen.exception.ChefNotFoundException;
import com.restaurant.kitchen.exception.TicketNotFoundException;
import com.restaurant.kitchen.mapper.ChefProfileMapper;
import com.restaurant.kitchen.mapper.KitchenTicketMapper;
import com.restaurant.kitchen.messaging.MessagePublisher;
import com.restaurant.kitchen.repository.ChefProfileRepository;
import com.restaurant.kitchen.repository.KitchenTicketRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class KitchenServiceImpl implements KitchenService {

    private final MessagePublisher publisher;

    private final ChefProfileRepository chefProfileRepository;
    private final KitchenTicketRepository kitchenTicketRepository;

    private final KitchenTicketMapper ticketMapper;
    private final ChefProfileMapper chefMapper;

    @Transactional(rollbackFor = Exception.class)
    @Override
    public void createChefProfile(UserCreatedEvent event, String correlationId, String sagaId) {

        log.info("Received userCreated event: {} , sagaID: {} , correlationID {}", event, sagaId, correlationId);

        if (!"CHEF".equals(event.getRole())) {
            log.info("Ignoring non-chef user: {}", event.getAuthUserId());
            return;  //spring Ack the message automatically since it did not throw an exception
        }

        Optional<ChefProfile> existingChef =
                chefProfileRepository.findByAuthUserId(event.getAuthUserId());

        if (existingChef.isPresent()) {

            log.info("Chef already exists, re-publishing creation event. userId: {}", event.getAuthUserId());

            publisher.publishChefCreated(
                    chefMapper.toProfileCreatedEvent(existingChef.get()),
                    sagaId, correlationId);

            return; //spring Ack the message automatically since it did not throw an exception
        }

        try {
            ChefProfile chef = chefProfileRepository.save(chefMapper.toEntity(event));
            publisher.publishChefCreated(chefMapper.toProfileCreatedEvent(chef), sagaId, correlationId);
        } catch (Exception exception) {
            log.error("Chef profile creation failed", exception); // due to publish failure or save failure

            // rollback DB if save succeeded but publish failed → prevents duplicate on retry
            // and nack the message to re-queue or route to dlq if max-attempts have been reached
            // when goes to dlq then publish user-created failed event to tell the engaged services to roll back
            throw new RuntimeException("Chef profile creation failed for userId: " + event.getAuthUserId());
        }
    }

    private ChefProfile findChef(Long id) {

        return chefProfileRepository.findById(id)
                .orElseThrow(() -> new ChefNotFoundException(id));

    }

    @Override
    public void updateDisplayName(Long id, String displayName) {

        ChefProfile retrievedChef = findChef(id);
        retrievedChef.setDisplayName(displayName.trim());
        chefProfileRepository.save(retrievedChef);
    }

    @Override
    public void updateStation(Long id, Station station) {

        ChefProfile retrievedChef = findChef(id);
        retrievedChef.setStation(station);
        chefProfileRepository.save(retrievedChef);
    }

    @Override
    public void updateChefStatus(Long id, ChefStatus status) {

        ChefProfile retrievedChef = findChef(id);
        retrievedChef.setStatus(status);
        chefProfileRepository.save(retrievedChef);
    }

    @Transactional(rollbackFor = Exception.class) //both of updating db and publishing event must success
    @Override
    public void updateTicketStatus(Long id, TicketStatus status) {

        KitchenTicket retrievedTicket = kitchenTicketRepository.findById(id)
                .orElseThrow(() -> new TicketNotFoundException(id));
        retrievedTicket.setStatus(status);
        kitchenTicketRepository.save(retrievedTicket);

        if (status.equals(TicketStatus.READY))
            publisher.publishTicketReady(
                    id,
                    ticketMapper.toTicketReadyEvent(retrievedTicket),
                    UUID.randomUUID().toString(),
                    UUID.randomUUID().toString());
    }

    @Override
    public List<KitchenTicketDTO> getActiveTickets() {
        List<KitchenTicket> activeTickets = kitchenTicketRepository.findByStatusNot(TicketStatus.READY);

        if (activeTickets.isEmpty())
            throw new TicketNotFoundException(null);

        return ticketMapper.toDTOList(activeTickets);
    }
}
