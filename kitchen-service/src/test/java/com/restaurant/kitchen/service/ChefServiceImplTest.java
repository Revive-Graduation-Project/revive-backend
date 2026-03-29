package com.restaurant.kitchen.service;

import com.restaurant.kitchen.dto.ChefProfileDTO;
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
        
        ChefProfileDTO dto = new ChefProfileDTO(1L, 10L, "NewName", Station.GRILL, ChefStatus.ACTIVE);

        when(chefProfileRepository.findById(1L)).thenReturn(Optional.of(chef));
        when(chefMapper.toDTO(chef)).thenReturn(dto);

        ChefProfileDTO result = chefService.updateChefDisplayName(1L, "NewName");

        assertEquals("NewName", chef.getDisplayName());
        assertEquals("NewName", result.displayName());
    }

    @Test
    void shouldTrimDisplayNameBeforeSaving() {
        ChefProfile chef = new ChefProfile();
        
        ChefProfileDTO dto = new ChefProfileDTO(1L, 10L, "NewName", Station.GRILL, ChefStatus.ACTIVE);

        when(chefProfileRepository.findById(1L)).thenReturn(Optional.of(chef));
        when(chefMapper.toDTO(chef)).thenReturn(dto);

        ChefProfileDTO result = chefService.updateChefDisplayName(1L, "  NewName  ");

        assertEquals("NewName", chef.getDisplayName());
        assertEquals("NewName", result.displayName());
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
        
        ChefProfileDTO dto = new ChefProfileDTO(1L, 10L, "John", Station.FRY, ChefStatus.ACTIVE);

        when(chefProfileRepository.findById(1L)).thenReturn(Optional.of(chef));
        when(chefMapper.toDTO(chef)).thenReturn(dto);

        ChefProfileDTO result = chefService.updateChefStation(1L, Station.FRY);

        assertEquals(Station.FRY, chef.getStation());
        assertEquals(Station.FRY, result.station());
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
        
        ChefProfileDTO dto = new ChefProfileDTO(1L, 10L, "John", Station.GRILL, ChefStatus.INACTIVE);

        when(chefProfileRepository.findById(1L)).thenReturn(Optional.of(chef));
        when(chefMapper.toDTO(chef)).thenReturn(dto);

        ChefProfileDTO result = chefService.updateChefStatus(1L, ChefStatus.INACTIVE);

        assertEquals(ChefStatus.INACTIVE, chef.getStatus());
        assertEquals(ChefStatus.INACTIVE, result.status());
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
        event.setId(1L);

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
        event.setId(1L);

        when(chefProfileRepository.findByAuthUserId(1L)).thenReturn(Optional.empty());

        chefService.createChefProfile(event, "corr-001", "saga-001");

        verify(chefProfileRepository).save(any(ChefProfile.class));
        verify(publisher).publishChefCreated(any(), eq("saga-001"), eq("corr-001"));
    }

    @Test
    void shouldThrowWhenSaveFails() {
        UserCreatedEvent event = new UserCreatedEvent();
        event.setRole("CHEF");
        event.setId(1L);

        when(chefProfileRepository.findByAuthUserId(1L)).thenReturn(Optional.empty());
        when(chefProfileRepository.save(any(ChefProfile.class))).thenThrow(new RuntimeException("DB error"));

        assertThrows(RuntimeException.class,
                () -> chefService.createChefProfile(event, "corr-001", "saga-001"));

        verifyNoInteractions(publisher);
    }
}
