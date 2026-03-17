package com.restaurant.kitchen.service;

import com.restaurant.kitchen.entity.ChefProfile;
import com.restaurant.kitchen.enums.ChefStatus;
import com.restaurant.kitchen.enums.Station;
import com.restaurant.kitchen.exception.ChefNotFoundException;
import com.restaurant.kitchen.repository.ChefProfileRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class KitchenServiceImplTest {

    @Mock
    private ChefProfileRepository chefProfileRepository;

    @InjectMocks
    private KitchenServiceImpl kitchenService; // ← only HTTP logic here

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
    void shouldThrowChefNotFoundWhenUpdatingDisplayNameOfNonExistentChef() {
        when(chefProfileRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ChefNotFoundException.class,
                () -> kitchenService.updateDisplayName(99L, "NewName"));

        verify(chefProfileRepository, never()).save(any());
    }

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
}