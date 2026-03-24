package com.restaurant.kitchen.service.impl;

import com.restaurant.kitchen.dto.KitchenTicketDTO;
import com.restaurant.kitchen.entity.ChefProfile;
import com.restaurant.kitchen.entity.KitchenTicket;
import com.restaurant.kitchen.enums.ChefStatus;
import com.restaurant.kitchen.enums.TicketStatus;
import com.restaurant.kitchen.events.OrderCreatedEvent;
import com.restaurant.kitchen.exception.TicketNotFoundException;
import com.restaurant.kitchen.mapper.KitchenTicketMapper;
import com.restaurant.kitchen.messaging.MessagePublisher;
import com.restaurant.kitchen.repository.ChefProfileRepository;
import com.restaurant.kitchen.repository.KitchenTicketRepository;
import com.restaurant.kitchen.service.TicketService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
public class TicketServiceImpl implements TicketService {

    private final KitchenTicketRepository kitchenTicketRepository;
    private final ChefProfileRepository chefProfileRepository;
    private final KitchenTicketMapper ticketMapper;
    private final MessagePublisher publisher;

    @Transactional(rollbackFor = Exception.class)
    @Override
    public void createKitchenTicket(OrderCreatedEvent event, String correlationId, String sagaId) {

        log.info("Received order.created event: {} , sagaID: {} , correlationID {}", event, sagaId, correlationId);

        Optional<KitchenTicket> existingTicket =
                kitchenTicketRepository.findByOrderId(event.getOrderId());

        // Idempotency check
        if (existingTicket.isPresent()) {

            log.info("Ticket already exists, re-publishing creation event. orderId: {}", event.getOrderId());

            publisher.publishTicketCreated(
                    ticketMapper.toTicketCreatedEvent(existingTicket.get()),
                    sagaId, correlationId);

            return; //spring Ack the message automatically since it did not throw an exception
        }

        try {
            KitchenTicket ticket = kitchenTicketRepository.save(
                    KitchenTicket.builder()  //manual fields setting is necessary therefore mapper is not used in this case
                            .orderId(event.getOrderId())
                            .status(TicketStatus.PENDING) // the initial state
                            .assignedChef(autoAssignChef())
                            .build()
            );
            publisher.publishTicketCreated(ticketMapper.toTicketCreatedEvent(ticket), sagaId, correlationId);
        } catch (Exception exception) {
            log.error("Chef profile creation failed", exception); // due to publish failure or save failure or unavailable chefs

            // rollback DB if save succeeded but publish failed → prevents duplicate on retry
            // and nack the message to re-queue or route to dlq if max-attempts have been reached
            // when goes to dlq then publish order-created failed event to tell the engaged services to roll back
            throw new RuntimeException("ticket creation failed for orderId: " + event.getOrderId());
        }
    }

    private ChefProfile autoAssignChef() {
        return chefProfileRepository
                .findByStatus(ChefStatus.ACTIVE) // find active chefs
                .stream()
                .min(Comparator.comparingInt(chef -> // return the chef with the fewest assigned tickets (the most available chef)
                        kitchenTicketRepository.countByAssignedChefAndStatusNot(chef, TicketStatus.READY)
                ))
                .orElseThrow(() -> new IllegalStateException("No available chefs to assign ticket")); // ticket cannot be created; therefore, nacking is necessary
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
                    ticketMapper.toTicketReadyEvent(retrievedTicket),
                    UUID.randomUUID().toString());
    }

    @Override
    public List<KitchenTicketDTO> getActiveTickets() {

        List<KitchenTicket> activeTickets = kitchenTicketRepository.findByStatusNot(TicketStatus.READY);
        return ticketMapper.toDTOList(activeTickets);
    }
}
