package com.restaurant.kitchen.service.impl;

import com.restaurant.kitchen.dto.KitchenTicketDTO;
import com.restaurant.kitchen.entity.ChefProfile;
import com.restaurant.kitchen.entity.KitchenTicket;
import com.restaurant.kitchen.enums.ChefStatus;
import com.restaurant.kitchen.enums.TicketStatus;
import com.restaurant.kitchen.events.OrderCreatedEvent;
import com.restaurant.kitchen.events.ticketEvents.TicketCanceledEvent;
import com.restaurant.kitchen.events.ticketEvents.TicketCreatedEvent;
import com.restaurant.kitchen.events.ticketEvents.TicketReadyEvent;
import com.restaurant.kitchen.events.ticketEvents.TicketStartedEvent;
import com.restaurant.kitchen.exception.TicketNotFoundException;
import com.restaurant.kitchen.mapper.KitchenTicketMapper;
import com.restaurant.kitchen.messaging.MessagePublisher;
import com.restaurant.kitchen.repository.ChefProfileRepository;
import com.restaurant.kitchen.repository.KitchenTicketRepository;
import com.restaurant.kitchen.service.TicketService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


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
                kitchenTicketRepository.findByOrderId(event.getId());

        // Idempotency check
        if (existingTicket.isPresent()) {

            log.info("Ticket already exists, re-publishing creation event. orderId: {}", event.getId());

            publisher.publishTicketCreated(
                    new TicketCreatedEvent(event.getId(), existingTicket.get().getId()),
                    sagaId, correlationId);

            return; //spring Ack the message automatically since it did not throw an exception
        }

        try {
            KitchenTicket ticket = kitchenTicketRepository.save(
                    KitchenTicket.builder()  //manual fields setting is necessary therefore mapper is not used in this case
                            .orderId(event.getId())
                            .assignedChef(autoAssignChef())
                            .build()
            );
            publisher.publishTicketCreated(
                    new TicketCreatedEvent(event.getId(), ticket.getId()),
                    sagaId, correlationId);
        } catch (Exception exception) {
            log.error("Kitchen ticket creation failed", exception); // due to publish failure or save failure or unavailable chefs

            // rollback DB if save succeeded but publish failed → prevents duplicate on retry
            // and nack the message to re-queue or route to dlq if max-attempts have been reached
            // when goes to dlq then publish order-created failed event to tell the engaged services to roll back
            throw new RuntimeException("Kitchen ticket creation failed for orderId: " + event.getId());
        }
    }

    private ChefProfile autoAssignChef() {
        return chefProfileRepository
                .findMostAvailableActiveChefs(
                        ChefStatus.ACTIVE,
                        List.of(TicketStatus.QUEUED, TicketStatus.PREPARING),
                        PageRequest.of(0, 1))
                .stream()
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("No available chefs to assign ticket")); // ticket cannot be created; therefore, nacking is necessary
    }

    @Transactional(rollbackFor = Exception.class) //both of updating db and publishing event must success
    @Override
    public KitchenTicketDTO updateTicketStatus(Long id, TicketStatus status) {


        KitchenTicket retrievedTicket = kitchenTicketRepository.findById(id)
                .orElseThrow(() -> new TicketNotFoundException(id));

        // Add before saving
        if (retrievedTicket.getStatus() == status) {
            log.info("Ticket {} already in status {}, skipping.", id, status);
            return ticketMapper.toDTO(retrievedTicket);
        }

        retrievedTicket.setStatus(status);
        kitchenTicketRepository.save(retrievedTicket);

        if (status == TicketStatus.PREPARING)
            publisher.publishTicketStarted(
                    new TicketStartedEvent(retrievedTicket.getOrderId(), id),
                    UUID.randomUUID().toString());

        else if (status == TicketStatus.READY)
            publisher.publishTicketReady(
                    new TicketReadyEvent(retrievedTicket.getOrderId(), id),
                    UUID.randomUUID().toString());

        return ticketMapper.toDTO(retrievedTicket);
    }

    @Transactional // Essential to ensure DB atomic commit
    public void cancelKitchenTicket(Long orderId, String sagaId, String correlationId) {
        kitchenTicketRepository.findByOrderId(orderId).ifPresentOrElse(
                ticket -> {

                    if (ticket.getStatus() == TicketStatus.CANCELED)
                    {
                        log.info("[SagaID: {}] Kitchen ticket {} is already canceled for order {}",
                                sagaId, ticket.getId(), orderId);
                        return;
                    }
                    // Check if the ticket is still in a state where cancellation is physically possible
                    if (ticket.getStatus() == TicketStatus.QUEUED) {
                        ticket.setStatus(TicketStatus.CANCELED);
                        kitchenTicketRepository.save(ticket);

                        publisher.publishTicketCanceled(new TicketCanceledEvent(orderId, ticket.getId()), correlationId);

                        log.info("[SagaID: {}] Kitchen ticket {} successfully cancelled for order {}",
                                sagaId, ticket.getId(), orderId);

                        // Optional: Publish a 'KitchenTicketCancelledEvent' here if the Saga expects a success reply
                    } else {
                        log.error("[SagaID: {}] Critical: Cannot cancel ticket for order {}. Kitchen status is already {}",
                                sagaId, orderId, ticket.getStatus());

                        // CRITICAL: Throw an exception to fail the transaction or notify your Saga manager
                        // that the cancellation compensation step failed!
                        throw new IllegalStateException("Kitchen ticket cannot be cancelled in status: " + ticket.getStatus());
                    }
                },
                () -> {
                    log.warn("[SagaID: {}] No kitchen ticket found for cancelled order {}", sagaId, orderId);
                    throw new IllegalStateException("Kitchen ticket not found for orderId: " + orderId);

                }
        );
    }

    @Transactional(readOnly = true)
    @Override
    public List<KitchenTicketDTO> getActiveTickets() {

        List<KitchenTicket> activeTickets = kitchenTicketRepository.findByStatusIn(
                List.of(TicketStatus.QUEUED, TicketStatus.PREPARING)
        );
        return ticketMapper.toDTOList(activeTickets);
    }
}
