package com.restaurant.kitchen.service;

import com.restaurant.kitchen.dto.KitchenTicketDTO;
import com.restaurant.kitchen.entity.ChefProfile;
import com.restaurant.kitchen.entity.KitchenTicket;
import com.restaurant.kitchen.enums.ChefStatus;
import com.restaurant.kitchen.enums.TicketStatus;
import com.restaurant.kitchen.events.OrderCreatedEvent;
import com.restaurant.kitchen.events.ticketEvents.TicketReadyEvent;
import com.restaurant.kitchen.events.ticketEvents.TicketStartedEvent;
import com.restaurant.kitchen.exception.TicketNotFoundException;
import com.restaurant.kitchen.mapper.KitchenTicketMapper;
import com.restaurant.kitchen.messaging.MessagePublisher;
import com.restaurant.kitchen.repository.ChefProfileRepository;
import com.restaurant.kitchen.repository.KitchenTicketRepository;
import com.restaurant.kitchen.service.impl.TicketServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TicketServiceImplTest {

    @Mock
    private KitchenTicketRepository kitchenTicketRepository;

    @Mock
    private ChefProfileRepository chefProfileRepository;

    @Mock
    private KitchenTicketMapper ticketMapper;

    @Mock
    private MessagePublisher publisher;

    @InjectMocks
    private TicketServiceImpl ticketService;

    // ── createKitchenTicket ──────────────────────────────────────────────

    @Test
    void shouldRepublishTicketCreatedWhenTicketAlreadyExists() {
        OrderCreatedEvent event = new OrderCreatedEvent();
        event.setId(100L);

        KitchenTicket existingTicket = buildTicket(TicketStatus.PREPARING);
        when(kitchenTicketRepository.findByOrderId(100L)).thenReturn(Optional.of(existingTicket));

        ticketService.createKitchenTicket(event, "corr-001", "saga-001");

        verify(kitchenTicketRepository, never()).save(any());
        verify(publisher).publishTicketCreated(any(), eq("saga-001"), eq("corr-001"));
    }

    @Test
    void shouldSaveAndPublishWhenTicketDoesNotExistAndChefAvailable() {
        OrderCreatedEvent event = new OrderCreatedEvent();
        event.setId(100L);

        ChefProfile availableChef = new ChefProfile();
        availableChef.setId(10L);

        when(kitchenTicketRepository.findByOrderId(100L)).thenReturn(Optional.empty());
        when(chefProfileRepository.findMostAvailableActiveChefs(
                ChefStatus.ACTIVE, List.of(TicketStatus.QUEUED, TicketStatus.PREPARING), PageRequest.of(0, 1)))
                .thenReturn(List.of(availableChef));

        KitchenTicket savedTicket = buildTicket(TicketStatus.PREPARING);
        savedTicket.setId(1L);
        when(kitchenTicketRepository.save(any(KitchenTicket.class))).thenReturn(savedTicket);

        ticketService.createKitchenTicket(event, "corr-001", "saga-001");

        verify(kitchenTicketRepository).save(any(KitchenTicket.class));
        verify(publisher).publishTicketCreated(any(), eq("saga-001"), eq("corr-001"));
    }

    @Test
    void shouldThrowWhenNoChefAvailable() {
        OrderCreatedEvent event = new OrderCreatedEvent();
        event.setId(100L);

        when(kitchenTicketRepository.findByOrderId(100L)).thenReturn(Optional.empty());
        when(chefProfileRepository.findMostAvailableActiveChefs(
                ChefStatus.ACTIVE, List.of(TicketStatus.QUEUED, TicketStatus.PREPARING), PageRequest.of(0, 1)))
                .thenReturn(List.of());

        assertThrows(RuntimeException.class, () -> ticketService.createKitchenTicket(event, "corr-001", "saga-001"));

        verify(kitchenTicketRepository, never()).save(any());
        verifyNoInteractions(publisher);
    }

    // ── updateTicketStatus ───────────────────────────────────────────────

    @Test
    void shouldUpdateTicketStatusSuccessfully() {
        KitchenTicket ticket = buildTicket(TicketStatus.PREPARING);
        KitchenTicketDTO dto = new KitchenTicketDTO(1L, 100L, TicketStatus.READY, 10L, LocalDateTime.now());

        when(kitchenTicketRepository.findById(1L)).thenReturn(Optional.of(ticket));
        when(ticketMapper.toDTO(ticket)).thenReturn(dto);

        KitchenTicketDTO result = ticketService.updateTicketStatus(1L, TicketStatus.READY);

        assertEquals(TicketStatus.READY, ticket.getStatus());
        assertEquals(TicketStatus.READY, result.status());
    }

    @Test
    void shouldPublishTicketReadyEventWhenStatusIsReady() {
        KitchenTicket ticket = buildTicket(TicketStatus.PREPARING);
        KitchenTicketDTO dto = new KitchenTicketDTO(1L, 100L, TicketStatus.READY, 10L, LocalDateTime.now());

        when(kitchenTicketRepository.findById(1L)).thenReturn(Optional.of(ticket));
        when(ticketMapper.toDTO(ticket)).thenReturn(dto);

        ticketService.updateTicketStatus(1L, TicketStatus.READY);

        verify(publisher).publishTicketReady(any(TicketReadyEvent.class), anyString());
    }

    @Test
    void shouldNotPublishEventWhenStatusIsNotReady() {
        KitchenTicket ticket = buildTicket(TicketStatus.PREPARING);
        KitchenTicketDTO dto = new KitchenTicketDTO(1L, 100L, TicketStatus.CANCELED, 10L, LocalDateTime.now());

        when(kitchenTicketRepository.findById(1L)).thenReturn(Optional.of(ticket));
        when(ticketMapper.toDTO(ticket)).thenReturn(dto);

        ticketService.updateTicketStatus(1L, TicketStatus.CANCELED);

        verifyNoInteractions(publisher);
    }

    @Test
    void shouldThrowTicketNotFoundWhenUpdatingStatusOfNonExistentTicket() {
        when(kitchenTicketRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(TicketNotFoundException.class,
                () -> ticketService.updateTicketStatus(99L, TicketStatus.READY));

        verify(kitchenTicketRepository, never()).save(any());
        verifyNoInteractions(publisher);
    }

    @Test
    void shouldPublishTicketStartedEventWhenStatusIsPreparing() {
        KitchenTicket ticket = buildTicket(TicketStatus.QUEUED);
        KitchenTicketDTO dto = new KitchenTicketDTO(1L, 100L, TicketStatus.PREPARING, 10L, LocalDateTime.now());

        when(kitchenTicketRepository.findById(1L)).thenReturn(Optional.of(ticket));
        when(ticketMapper.toDTO(ticket)).thenReturn(dto);

        ticketService.updateTicketStatus(1L, TicketStatus.PREPARING);

        verify(publisher).publishTicketStarted(any(TicketStartedEvent.class), anyString());
    }

    @Test
    void shouldNotPublishEventWhenStatusUnchanged() {
        KitchenTicket ticket = buildTicket(TicketStatus.PREPARING);
        KitchenTicketDTO dto = new KitchenTicketDTO(1L, 100L, TicketStatus.PREPARING, 10L, LocalDateTime.now());

        when(kitchenTicketRepository.findById(1L)).thenReturn(Optional.of(ticket));
        when(ticketMapper.toDTO(ticket)).thenReturn(dto);

        KitchenTicketDTO result = ticketService.updateTicketStatus(1L, TicketStatus.PREPARING);

        assertEquals(TicketStatus.PREPARING, ticket.getStatus());
        verifyNoInteractions(publisher);
    }

    // ── cancelKitchenTicket ────────────────────────────────────────────────

    @Test
    void shouldCancelTicketSuccessfullyWhenQueued() {
        KitchenTicket ticket = buildTicket(TicketStatus.QUEUED);
        when(kitchenTicketRepository.findByOrderId(100L)).thenReturn(Optional.of(ticket));

        ticketService.cancelKitchenTicket(100L, "saga-001", "corr-001");

        assertEquals(TicketStatus.CANCELED, ticket.getStatus());
        verify(kitchenTicketRepository).save(ticket);
        verify(publisher).publishTicketCanceled(any(), eq("corr-001"));
    }

    @Test
    void shouldNotCancelTicketWhenAlreadyCanceled() {
        KitchenTicket ticket = buildTicket(TicketStatus.CANCELED);
        when(kitchenTicketRepository.findByOrderId(100L)).thenReturn(Optional.of(ticket));

        ticketService.cancelKitchenTicket(100L, "saga-001", "corr-001");

        assertEquals(TicketStatus.CANCELED, ticket.getStatus());
        verify(kitchenTicketRepository, never()).save(any());
        verify(publisher, never()).publishTicketCanceled(any(), anyString());
    }

    @Test
    void shouldThrowWhenCancelingNonQueuedTicket() {
        KitchenTicket ticket = buildTicket(TicketStatus.PREPARING);
        when(kitchenTicketRepository.findByOrderId(100L)).thenReturn(Optional.of(ticket));

        assertThrows(IllegalStateException.class,
                () -> ticketService.cancelKitchenTicket(100L, "saga-001", "corr-001"));

        verify(kitchenTicketRepository, never()).save(any());
        verify(publisher, never()).publishTicketCanceled(any(), anyString());
    }

    @Test
    void shouldThrowWhenTicketNotFoundForCancellation() {
        when(kitchenTicketRepository.findByOrderId(99L)).thenReturn(Optional.empty());

        assertThrows(IllegalStateException.class,
                () -> ticketService.cancelKitchenTicket(99L, "saga-001", "corr-001"));

        verify(kitchenTicketRepository, never()).save(any());
        verify(publisher, never()).publishTicketCanceled(any(), anyString());
    }

    // ── getActiveTickets ─────────────────────────────────────────────────

    @Test
    void shouldReturnActiveTicketsSuccessfully() {
        KitchenTicket ticket = buildTicket(TicketStatus.PREPARING);
        KitchenTicketDTO dto = new KitchenTicketDTO(1L, 100L, TicketStatus.PREPARING, 10L, LocalDateTime.now());

        when(kitchenTicketRepository.findByStatusIn(List.of(TicketStatus.QUEUED, TicketStatus.PREPARING))).thenReturn(List.of(ticket));
        when(ticketMapper.toDTOList(List.of(ticket))).thenReturn(List.of(dto));

        List<KitchenTicketDTO> result = ticketService.getActiveTickets();

        assertEquals(1, result.size());
        assertEquals(TicketStatus.PREPARING, result.get(0).status());
    }

    @Test
    void shouldReturnEmptyListWhenNoActiveTicketsExist() {
        when(kitchenTicketRepository.findByStatusIn(List.of(TicketStatus.QUEUED, TicketStatus.PREPARING))).thenReturn(List.of());
        when(ticketMapper.toDTOList(List.of())).thenReturn(List.of());

        List<KitchenTicketDTO> result = ticketService.getActiveTickets();

        assertEquals(0, result.size());
    }

    // ── helper ───────────────────────────────────────────────────────────

    private KitchenTicket buildTicket(TicketStatus status) {
        ChefProfile chef = new ChefProfile();
        chef.setId(10L);
        chef.setDisplayName("Chef John");

        return KitchenTicket.builder()
                .id(1L)
                .orderId(100L)
                .status(status)
                .assignedChef(chef)
                .createdAt(LocalDateTime.now())
                .build();
    }
}