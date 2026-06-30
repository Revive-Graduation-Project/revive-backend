package com.restaurant.kitchen.service;

import com.restaurant.kitchen.dto.ChefProfileDTO;
import com.restaurant.kitchen.entity.ChefProfile;
import com.restaurant.kitchen.enums.ChefStatus;
import com.restaurant.kitchen.enums.Station;
import com.restaurant.kitchen.events.UserCreatedEvent;
import com.restaurant.kitchen.events.chefProfileEvents.ProfileCreatedEvent;
import com.restaurant.kitchen.exception.ChefNotFoundException;
import com.restaurant.kitchen.mapper.ChefProfileMapper;
import com.restaurant.kitchen.messaging.MessagePublisher;
import com.restaurant.kitchen.repository.ChefProfileRepository;
import com.restaurant.kitchen.service.impl.ChefServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

class ChefServiceTest {

    @Mock
    private ChefProfileRepository chefProfileRepository;

    @Mock
    private ChefProfileMapper chefProfileMapper;

    @Mock
    private MessagePublisher publisher;

    @InjectMocks
    private ChefServiceImpl chefService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void createChefProfileSuccessfullyWhenUserIsChefAndProfileDoesNotExist() {
        UserCreatedEvent event = new UserCreatedEvent();
        event.setId(1L);
        event.setRole("CHEF");

        when(chefProfileRepository.findByAuthUserId(1L)).thenReturn(Optional.empty());

        chefService.createChefProfile(event, "corr123", "saga123");

        verify(chefProfileRepository, times(1)).save(any(ChefProfile.class));
        verify(publisher, times(1)).publishChefCreated(any(ProfileCreatedEvent.class), eq("saga123"), eq("corr123"));
    }

    @Test
    void ignoreChefProfileCreationWhenUserIsNotChef() {
        UserCreatedEvent event = new UserCreatedEvent();
        event.setId(1L);
        event.setRole("CUSTOMER");

        chefService.createChefProfile(event, "corr123", "saga123");

        verify(chefProfileRepository, never()).findByAuthUserId(anyLong());
        verify(chefProfileRepository, never()).save(any());
        verify(publisher, never()).publishChefCreated(any(), any(), any());
    }

    @Test
    void republishCreationEventWhenChefProfileAlreadyExists() {
        UserCreatedEvent event = new UserCreatedEvent();
        event.setId(1L);
        event.setRole("CHEF");

        ChefProfile existingChef = new ChefProfile();
        existingChef.setId(10L);
        existingChef.setAuthUserId(1L);

        when(chefProfileRepository.findByAuthUserId(1L)).thenReturn(Optional.of(existingChef));

        chefService.createChefProfile(event, "corr123", "saga123");

        verify(chefProfileRepository, never()).save(any());
        verify(publisher, times(1)).publishChefCreated(any(ProfileCreatedEvent.class), eq("saga123"), eq("corr123"));
    }

    @Test
    void throwRuntimeExceptionWhenChefProfileCreationFailsOnDatabaseSave() {
        UserCreatedEvent event = new UserCreatedEvent();
        event.setId(1L);
        event.setRole("CHEF");

        when(chefProfileRepository.findByAuthUserId(1L)).thenReturn(Optional.empty());
        when(chefProfileRepository.save(any())).thenThrow(new RuntimeException("DB error"));

        assertThrows(RuntimeException.class, () -> 
            chefService.createChefProfile(event, "corr123", "saga123")
        );

        verify(publisher, never()).publishChefCreated(any(), any(), any());
    }

    @Test
    void throwRuntimeExceptionWhenChefProfileCreationFailsOnPublishingEvent() {
        UserCreatedEvent event = new UserCreatedEvent();
        event.setId(1L);
        event.setRole("CHEF");

        when(chefProfileRepository.findByAuthUserId(1L)).thenReturn(Optional.empty());
        doThrow(new RuntimeException("Publish error"))
                .when(publisher).publishChefCreated(any(), any(), any());

        assertThrows(RuntimeException.class, () -> 
            chefService.createChefProfile(event, "corr123", "saga123")
        );

        verify(chefProfileRepository, times(1)).save(any());
    }

    @Test
    void updateChefDisplayNameSuccessfullyWhenChefExists() {
        Long chefId = 1L;
        String newDisplayName = "New Display Name";
        ChefProfile chef = new ChefProfile();
        chef.setId(chefId);
        chef.setDisplayName("Old Display Name");
        ChefProfileDTO dto = new ChefProfileDTO(chefId, 1L, "New Display Name", Station.UNASSIGNED, ChefStatus.ACTIVE);

        when(chefProfileRepository.findById(chefId)).thenReturn(Optional.of(chef));
        when(chefProfileMapper.toDTO(chef)).thenReturn(dto);

        ChefProfileDTO result = chefService.updateChefDisplayName(chefId, newDisplayName);

        assertEquals(newDisplayName, result.displayName());
        verify(chefProfileRepository, times(1)).findById(chefId);
    }

    @Test
    void throwChefNotFoundExceptionWhenUpdatingDisplayNameForNonExistentChef() {
        Long chefId = 1L;
        when(chefProfileRepository.findById(chefId)).thenReturn(Optional.empty());

        assertThrows(ChefNotFoundException.class, () -> 
            chefService.updateChefDisplayName(chefId, "Some Name")
        );

        verify(chefProfileRepository, times(1)).findById(chefId);
        verifyNoInteractions(chefProfileMapper);
    }

    @Test
    void updateChefStationSuccessfullyWhenChefExists() {
        Long chefId = 1L;
        Station newStation = Station.PASTRY;
        ChefProfile chef = new ChefProfile();
        chef.setId(chefId);
        chef.setStation(Station.GRILL);
        ChefProfileDTO dto = new ChefProfileDTO(chefId, 1L, "Chef John", Station.PASTRY, ChefStatus.ACTIVE);

        when(chefProfileRepository.findById(chefId)).thenReturn(Optional.of(chef));
        when(chefProfileMapper.toDTO(chef)).thenReturn(dto);

        ChefProfileDTO result = chefService.updateChefStation(chefId, newStation);

        assertEquals(newStation, result.station());
        verify(chefProfileRepository, times(1)).findById(chefId);
    }

    @Test
    void throwChefNotFoundExceptionWhenUpdatingStationForNonExistentChef() {
        Long chefId = 1L;
        when(chefProfileRepository.findById(chefId)).thenReturn(Optional.empty());

        assertThrows(ChefNotFoundException.class, () -> 
            chefService.updateChefStation(chefId, Station.PASTRY)
        );

        verify(chefProfileRepository, times(1)).findById(chefId);
        verifyNoInteractions(chefProfileMapper);
    }

    @Test
    void updateChefStatusSuccessfullyWhenChefExists() {
        Long chefId = 1L;
        ChefStatus newStatus = ChefStatus.ACTIVE;
        ChefProfile chef = new ChefProfile();
        chef.setId(chefId);
        chef.setStatus(ChefStatus.INACTIVE);
        ChefProfileDTO dto = new ChefProfileDTO(chefId, 1L, "Chef John", Station.UNASSIGNED, ChefStatus.ACTIVE);

        when(chefProfileRepository.findById(chefId)).thenReturn(Optional.of(chef));
        when(chefProfileMapper.toDTO(chef)).thenReturn(dto);

        ChefProfileDTO result = chefService.updateChefStatus(chefId, newStatus);

        assertEquals(newStatus, result.status());
        verify(chefProfileRepository, times(1)).findById(chefId);
    }

    @Test
    void throwChefNotFoundExceptionWhenUpdatingStatusForNonExistentChef() {
        Long chefId = 1L;
        when(chefProfileRepository.findById(chefId)).thenReturn(Optional.empty());

        assertThrows(ChefNotFoundException.class, () -> 
            chefService.updateChefStatus(chefId, ChefStatus.ACTIVE)
        );

        verify(chefProfileRepository, times(1)).findById(chefId);
        verifyNoInteractions(chefProfileMapper);
    }
}
