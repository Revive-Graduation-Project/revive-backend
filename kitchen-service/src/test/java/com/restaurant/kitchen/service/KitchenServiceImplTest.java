package com.restaurant.kitchen.service;

import com.restaurant.kitchen.dto.KitchenTicketDTO;
import com.restaurant.kitchen.entity.ChefProfile;
import com.restaurant.kitchen.entity.KitchenTicket;
import com.restaurant.kitchen.enums.ChefStatus;
import com.restaurant.kitchen.enums.Station;
import com.restaurant.kitchen.enums.TicketStatus;
import com.restaurant.kitchen.events.TicketReadyEvent;
import com.restaurant.kitchen.events.UserCreatedEvent;
import com.restaurant.kitchen.exception.ChefNotFoundException;
import com.restaurant.kitchen.exception.TicketNotFoundException;
import com.restaurant.kitchen.mapper.ChefProfileMapper;
import com.restaurant.kitchen.mapper.KitchenTicketMapper;
import com.restaurant.kitchen.messaging.MessagePublisher;
import com.restaurant.kitchen.repository.ChefProfileRepository;
import com.restaurant.kitchen.repository.KitchenTicketRepository;
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
class KitchenServiceImplTest {

    @Mock
    private ChefProfileRepository chefProfileRepository;

    @Mock
    private KitchenTicketRepository kitchenTicketRepository;

    @Mock
    private KitchenTicketMapper ticketMapper;

    @Mock
    private ChefProfileMapper chefMapper;

    @Mock
    private MessagePublisher publisher;

    @InjectMocks
    private KitchenServiceImpl kitchenService;

    // ── updateDisplayName ────────────────────────────────────────────────

    @Test
    void shouldUpdateDisplayNameSuccessfully() {
        ChefProfile chef = new ChefProfile();
        chef.setDisplayName("OldName");

        when(chefProfileRepository.findById(1L)).thenReturn(Optional.of(chef));

        kitchenService.updateDisplayName(1L, "NewName");

        assertEquals("NewName", chef.getDisplayName());
        verify(chefProfileRepository).save(chef);
    }

    @Test
    void shouldTrimDisplayNameBeforeSaving() {
        ChefProfile chef = new ChefProfile();

        when(chefProfileRepository.findById(1L)).thenReturn(Optional.of(chef));

        kitchenService.updateDisplayName(1L, "  NewName  ");

        assertEquals("NewName", chef.getDisplayName());
        verify(chefProfileRepository).save(chef);
    }

    @Test
    void shouldThrowChefNotFoundWhenUpdatingDisplayNameOfNonExistentChef() {
        when(chefProfileRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ChefNotFoundException.class,
                () -> kitchenService.updateDisplayName(99L, "NewName"));

        verify(chefProfileRepository, never()).save(any());
    }

    // ── updateStation ────────────────────────────────────────────────────

    @Test
    void shouldUpdateStationSuccessfully() {
        ChefProfile chef = new ChefProfile();
        chef.setStation(Station.GRILL);

        when(chefProfileRepository.findById(1L)).thenReturn(Optional.of(chef));

        kitchenService.updateStation(1L, Station.FRY);

        assertEquals(Station.FRY, chef.getStation());
        verify(chefProfileRepository).save(chef);
    }

    @Test
    void shouldThrowChefNotFoundWhenUpdatingStationOfNonExistentChef() {
        when(chefProfileRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ChefNotFoundException.class,
                () -> kitchenService.updateStation(99L, Station.GRILL));

        verify(chefProfileRepository, never()).save(any());
    }

    // ── updateChefStatus ─────────────────────────────────────────────────

    @Test
    void shouldUpdateChefStatusSuccessfully() {
        ChefProfile chef = new ChefProfile();
        chef.setStatus(ChefStatus.ACTIVE);

        when(chefProfileRepository.findById(1L)).thenReturn(Optional.of(chef));

        kitchenService.updateChefStatus(1L, ChefStatus.INACTIVE);

        assertEquals(ChefStatus.INACTIVE, chef.getStatus());
        verify(chefProfileRepository).save(chef);
    }

    @Test
    void shouldThrowChefNotFoundWhenUpdatingStatusOfNonExistentChef() {
        when(chefProfileRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ChefNotFoundException.class,
                () -> kitchenService.updateChefStatus(99L, ChefStatus.INACTIVE));

        verify(chefProfileRepository, never()).save(any());
    }

    // ── updateTicketStatus ───────────────────────────────────────────────

    @Test
    void shouldUpdateTicketStatusSuccessfully() {
        KitchenTicket ticket = buildTicket(TicketStatus.PENDING);

        when(kitchenTicketRepository.findById(1L)).thenReturn(Optional.of(ticket));

        kitchenService.updateTicketStatus(1L, TicketStatus.COOKING);

        assertEquals(TicketStatus.COOKING, ticket.getStatus());
        verify(kitchenTicketRepository).save(ticket);
    }

    @Test
    void shouldPublishTicketReadyEventWhenStatusIsReady() {
        KitchenTicket ticket = buildTicket(TicketStatus.COOKING);
        TicketReadyEvent event = new TicketReadyEvent();

        when(kitchenTicketRepository.findById(1L)).thenReturn(Optional.of(ticket));
        when(ticketMapper.toTicketReadyEvent(ticket)).thenReturn(event);

        kitchenService.updateTicketStatus(1L, TicketStatus.READY);

        verify(publisher).publishTicketReady(eq(1L), eq(event), any(), any());
    }

    @Test
    void shouldNotPublishEventWhenStatusIsNotReady() {
        KitchenTicket ticket = buildTicket(TicketStatus.PENDING);

        when(kitchenTicketRepository.findById(1L)).thenReturn(Optional.of(ticket));

        kitchenService.updateTicketStatus(1L, TicketStatus.COOKING);

        verifyNoInteractions(publisher);
    }

    @Test
    void shouldThrowTicketNotFoundWhenUpdatingStatusOfNonExistentTicket() {
        when(kitchenTicketRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(TicketNotFoundException.class,
                () -> kitchenService.updateTicketStatus(99L, TicketStatus.READY));

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

        List<KitchenTicketDTO> result = kitchenService.getActiveTickets();

        assertEquals(1, result.size());
        assertEquals(TicketStatus.COOKING, result.get(0).status());
    }

    @Test
    void shouldThrowTicketNotFoundWhenNoActiveTicketsExist() {
        when(kitchenTicketRepository.findByStatusNot(TicketStatus.READY)).thenReturn(List.of());

        assertThrows(TicketNotFoundException.class,
                () -> kitchenService.getActiveTickets());

        verifyNoInteractions(ticketMapper);
    }

    // ── createChefProfile ────────────────────────────────────────────────

    @Test
    void shouldIgnoreNonChefUser() {
        UserCreatedEvent event = new UserCreatedEvent();
        event.setRole("CUSTOMER");

        kitchenService.createChefProfile(event, "corr-001", "saga-001");

        verifyNoInteractions(chefProfileRepository);
        verifyNoInteractions(publisher);
    }

    @Test
    void shouldRepublishChefCreatedWhenChefAlreadyExists() {
        UserCreatedEvent event = new UserCreatedEvent();
        event.setRole("CHEF");
        event.setAuthUserId(1L);

        ChefProfile existingChef = new ChefProfile();
        when(chefProfileRepository.findByAuthUserId(1L)).thenReturn(Optional.of(existingChef));

        kitchenService.createChefProfile(event, "corr-001", "saga-001");

        verify(chefProfileRepository, never()).save(any());
        verify(publisher).publishChefCreated(any(), eq("saga-001"), eq("corr-001"));
    }

    @Test
    void shouldSaveAndPublishWhenChefDoesNotExist() {
        UserCreatedEvent event = new UserCreatedEvent();
        event.setRole("CHEF");
        event.setAuthUserId(1L);

        ChefProfile mappedChef = new ChefProfile();
        ChefProfile savedChef = new ChefProfile();

        when(chefProfileRepository.findByAuthUserId(1L)).thenReturn(Optional.empty());
        when(chefMapper.toEntity(event)).thenReturn(mappedChef);
        when(chefProfileRepository.save(mappedChef)).thenReturn(savedChef);

        kitchenService.createChefProfile(event, "corr-001", "saga-001");

        verify(chefProfileRepository).save(mappedChef);
        verify(publisher).publishChefCreated(any(), eq("saga-001"), eq("corr-001"));
    }

    @Test
    void shouldThrowWhenSaveFails() {
        UserCreatedEvent event = new UserCreatedEvent();
        event.setRole("CHEF");
        event.setAuthUserId(1L);

        when(chefProfileRepository.findByAuthUserId(1L)).thenReturn(Optional.empty());
        when(chefMapper.toEntity(event)).thenReturn(new ChefProfile());
        when(chefProfileRepository.save(any())).thenThrow(new RuntimeException("DB error"));

        assertThrows(RuntimeException.class,
                () -> kitchenService.createChefProfile(event, "corr-001", "saga-001"));

        verifyNoInteractions(publisher);
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