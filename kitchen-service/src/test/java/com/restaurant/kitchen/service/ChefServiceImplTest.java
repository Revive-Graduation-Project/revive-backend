package com.restaurant.kitchen.service;

import com.restaurant.kitchen.entity.ChefProfile;
import com.restaurant.kitchen.enums.ChefStatus;
import com.restaurant.kitchen.enums.Station;
import com.restaurant.kitchen.events.UserCreatedEvent;
import com.restaurant.kitchen.exception.ChefNotFoundException;
import com.restaurant.kitchen.mapper.ChefProfileMapper;
import com.restaurant.kitchen.messaging.MessagePublisher;
import com.restaurant.kitchen.repository.ChefProfileRepository;
import com.restaurant.kitchen.service.impl.ChefServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ChefServiceImplTest {

    @Mock
    private ChefProfileRepository chefProfileRepository;

    @Mock
    private ChefProfileMapper chefMapper;

    @Mock
    private MessagePublisher publisher;

    @InjectMocks
    private ChefServiceImpl chefService;

    // ── updateDisplayName ────────────────────────────────────────────────

    @Test
    void shouldUpdateDisplayNameSuccessfully() {
        ChefProfile chef = new ChefProfile();
        chef.setDisplayName("OldName");

        when(chefProfileRepository.findById(1L)).thenReturn(Optional.of(chef));

        chefService.updateChefDisplayName(1L, "NewName");

        assertEquals("NewName", chef.getDisplayName());
    }

    @Test
    void shouldTrimDisplayNameBeforeSaving() {
        ChefProfile chef = new ChefProfile();

        when(chefProfileRepository.findById(1L)).thenReturn(Optional.of(chef));

        chefService.updateChefDisplayName(1L, "  NewName  ");

        assertEquals("NewName", chef.getDisplayName());
    }

    @Test
    void shouldThrowChefNotFoundWhenUpdatingDisplayNameOfNonExistentChef() {
        when(chefProfileRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ChefNotFoundException.class,
                () -> chefService.updateChefDisplayName(99L, "NewName"));

        verify(chefProfileRepository, never()).save(any());
    }

    // ── updateStation ────────────────────────────────────────────────────

    @Test
    void shouldUpdateStationSuccessfully() {
        ChefProfile chef = new ChefProfile();
        chef.setStation(Station.GRILL);

        when(chefProfileRepository.findById(1L)).thenReturn(Optional.of(chef));

        chefService.updateChefStation(1L, Station.FRY);

        assertEquals(Station.FRY, chef.getStation());
    }

    @Test
    void shouldThrowChefNotFoundWhenUpdatingStationOfNonExistentChef() {
        when(chefProfileRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ChefNotFoundException.class,
                () -> chefService.updateChefStation(99L, Station.GRILL));

        verify(chefProfileRepository, never()).save(any());
    }

    // ── updateChefStatus ─────────────────────────────────────────────────

    @Test
    void shouldUpdateChefStatusSuccessfully() {
        ChefProfile chef = new ChefProfile();
        chef.setStatus(ChefStatus.ACTIVE);

        when(chefProfileRepository.findById(1L)).thenReturn(Optional.of(chef));

        chefService.updateChefStatus(1L, ChefStatus.INACTIVE);

        assertEquals(ChefStatus.INACTIVE, chef.getStatus());
    }

    @Test
    void shouldThrowChefNotFoundWhenUpdatingStatusOfNonExistentChef() {
        when(chefProfileRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ChefNotFoundException.class,
                () -> chefService.updateChefStatus(99L, ChefStatus.INACTIVE));

        verify(chefProfileRepository, never()).save(any());
    }

    // ── createChefProfile ────────────────────────────────────────────────

    @Test
    void shouldIgnoreNonChefUser() {
        UserCreatedEvent event = new UserCreatedEvent();
        event.setRole("CUSTOMER");

        chefService.createChefProfile(event, "corr-001", "saga-001");

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

        chefService.createChefProfile(event, "corr-001", "saga-001");

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

        chefService.createChefProfile(event, "corr-001", "saga-001");

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
                () -> chefService.createChefProfile(event, "corr-001", "saga-001"));

        verifyNoInteractions(publisher);
    }
}
