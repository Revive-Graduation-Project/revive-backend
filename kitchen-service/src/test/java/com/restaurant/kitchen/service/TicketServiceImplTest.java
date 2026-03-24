package com.restaurant.kitchen.service;

import com.restaurant.kitchen.dto.KitchenTicketDTO;
import com.restaurant.kitchen.entity.ChefProfile;
import com.restaurant.kitchen.entity.KitchenTicket;
import com.restaurant.kitchen.enums.TicketStatus;
import com.restaurant.kitchen.events.ticketEvents.TicketReadyEvent;
import com.restaurant.kitchen.exception.TicketNotFoundException;
import com.restaurant.kitchen.mapper.KitchenTicketMapper;
import com.restaurant.kitchen.messaging.MessagePublisher;
import com.restaurant.kitchen.repository.KitchenTicketRepository;
import com.restaurant.kitchen.service.impl.TicketServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

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
    private KitchenTicketMapper ticketMapper;

    @Mock
    private MessagePublisher publisher;

    @InjectMocks
    private TicketServiceImpl ticketService;

    // ── updateTicketStatus ───────────────────────────────────────────────

    @Test
    void shouldUpdateTicketStatusSuccessfully() {
        KitchenTicket ticket = buildTicket(TicketStatus.PENDING);

        when(kitchenTicketRepository.findById(1L)).thenReturn(Optional.of(ticket));

        ticketService.updateTicketStatus(1L, TicketStatus.COOKING);

        assertEquals(TicketStatus.COOKING, ticket.getStatus());
        verify(kitchenTicketRepository).save(ticket);
    }

    @Test
    void shouldPublishTicketReadyEventWhenStatusIsReady() {
        KitchenTicket ticket = buildTicket(TicketStatus.COOKING);
        TicketReadyEvent event = new TicketReadyEvent();

        when(kitchenTicketRepository.findById(1L)).thenReturn(Optional.of(ticket));
        when(ticketMapper.toTicketReadyEvent(ticket)).thenReturn(event);

        ticketService.updateTicketStatus(1L, TicketStatus.READY);

        verify(publisher).publishTicketReady(eq(event), any());
    }

    @Test
    void shouldNotPublishEventWhenStatusIsNotReady() {
        KitchenTicket ticket = buildTicket(TicketStatus.PENDING);

        when(kitchenTicketRepository.findById(1L)).thenReturn(Optional.of(ticket));

        ticketService.updateTicketStatus(1L, TicketStatus.COOKING);

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

    // ── getActiveTickets ─────────────────────────────────────────────────

    @Test
    void shouldReturnActiveTicketsSuccessfully() {
        KitchenTicket ticket = buildTicket(TicketStatus.COOKING);
        KitchenTicketDTO dto = new KitchenTicketDTO(100L, TicketStatus.COOKING, "Chef John", LocalDateTime.now());

        when(kitchenTicketRepository.findByStatusNot(TicketStatus.READY)).thenReturn(List.of(ticket));
        when(ticketMapper.toDTOList(List.of(ticket))).thenReturn(List.of(dto));

        List<KitchenTicketDTO> result = ticketService.getActiveTickets();

        assertEquals(1, result.size());
        assertEquals(TicketStatus.COOKING, result.get(0).status());
    }

    @Test
    void shouldReturnEmptyListWhenNoActiveTicketsExist() {
        when(kitchenTicketRepository.findByStatusNot(TicketStatus.READY)).thenReturn(List.of());
        when(ticketMapper.toDTOList(List.of())).thenReturn(List.of());

        List<KitchenTicketDTO> result = ticketService.getActiveTickets();

        assertEquals(0, result.size());
    }

    // ── helper ───────────────────────────────────────────────────────────

    private KitchenTicket buildTicket(TicketStatus status) {
        ChefProfile chef = new ChefProfile();
        chef.setId(1L);
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
