package com.restaurant.kitchen.service;

import com.restaurant.kitchen.entity.ChefProfile;
import com.restaurant.kitchen.enums.ChefStatus;
import com.restaurant.kitchen.enums.Station;
import com.restaurant.kitchen.events.ProfileCreationFailedEvent;
import com.restaurant.kitchen.events.UserCreatedEvent;
import com.restaurant.kitchen.exception.ChefNotFoundException;
import com.restaurant.kitchen.mapper.ChefProfileMapper;
import com.restaurant.kitchen.repository.ChefProfileRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.core.MessagePostProcessor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class KitchenServiceTest {

    @Mock
    private RabbitTemplate rabbitTemplate;

    @Mock
    private ChefProfileRepository chefProfileRepository;

    @Mock
    private ChefProfileMapper mapper;

    @InjectMocks
    private KitchenService kitchenService;

    // inject @Value field manually
    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(kitchenService, "exchange", "test.exchange");
    }

    // ── Scenario 1 ──────────────────────────────────────────────────────
    @Test
    void shouldIgnoreNonChefUser() {
        // Arrange
        UserCreatedEvent event = new UserCreatedEvent();
        event.setRole("CUSTOMER");

        // Act
        kitchenService.createChefProfile(event, "corr-001", "saga-001");

        // Assert
        verifyNoInteractions(chefProfileRepository);
        verifyNoInteractions(rabbitTemplate);
    }

    // ── Scenario 2 ──────────────────────────────────────────────────────
    @Test
    void shouldRepublishCreatedEventWhenChefAlreadyExists() {
        // Arrange
        UserCreatedEvent event = new UserCreatedEvent();
        event.setRole("CHEF");
        event.setAuthUserId(1L);

        ChefProfile existingChef = new ChefProfile();
        when(chefProfileRepository.findByAuthUserId(1L))
                .thenReturn(Optional.of(existingChef));

        // Act
        kitchenService.createChefProfile(event, "corr-001", "saga-001");

        // Assert
        verify(chefProfileRepository, never()).save(any());
        verify(rabbitTemplate).convertAndSend(
                eq("test.exchange"),
                eq("chef-profile.created"),
                any(),
                any(MessagePostProcessor.class));
    }

    // ── Scenario 3 ──────────────────────────────────────────────────────
    @Test
    void shouldSaveAndPublishCreatedEventWhenChefDoesNotExist() {
        // Arrange
        UserCreatedEvent event = new UserCreatedEvent();
        event.setRole("CHEF");
        event.setAuthUserId(1L);

        ChefProfile mappedChef = new ChefProfile();
        ChefProfile savedChef = new ChefProfile();

        when(chefProfileRepository.findByAuthUserId(1L))
                .thenReturn(Optional.empty());
        when(mapper.toEntity(event))
                .thenReturn(mappedChef);
        when(chefProfileRepository.save(mappedChef))
                .thenReturn(savedChef);

        // Act
        kitchenService.createChefProfile(event, "corr-001", "saga-001");

        // Assert
        verify(chefProfileRepository).save(mappedChef);
        verify(rabbitTemplate).convertAndSend(
                eq("test.exchange"),
                eq("chef-profile.created"),
                any(),
                any(MessagePostProcessor.class));
    }

    // ── Scenario 4 ──────────────────────────────────────────────────────
    @Test
    void shouldPublishFailedEventWhenSaveThrowsException() {
        // Arrange
        UserCreatedEvent event = new UserCreatedEvent();
        event.setRole("CHEF");
        event.setAuthUserId(1L);

        when(chefProfileRepository.findByAuthUserId(1L))
                .thenReturn(Optional.empty());
        when(mapper.toEntity(event))
                .thenReturn(new ChefProfile());
        when(chefProfileRepository.save(any()))
                .thenThrow(new DataIntegrityViolationException("DB error"));

        ProfileCreationFailedEvent failedEvent = new ProfileCreationFailedEvent();
        when(mapper.toProfileCreationFailedEvent(event))
                .thenReturn(failedEvent);

        // Act
        kitchenService.createChefProfile(event, "corr-001", "saga-001");

        // Assert
        verify(rabbitTemplate).convertAndSend(
                eq("test.exchange"),
                eq("chef-profile.failed"),
                any(),
                any(MessagePostProcessor.class));
    }


    // ── updateDisplayName ────────────────────────────────────────────────
    @Test
    void shouldUpdateDisplayNameSuccessfully() {
        // Arrange
        ChefProfile chef = new ChefProfile();
        chef.setDisplayName("OldName");

        when(chefProfileRepository.findById(1L))
                .thenReturn(Optional.of(chef));

        // Act
        kitchenService.updateDisplayName(1L, "NewName");

        // Assert
        assertEquals("NewName", chef.getDisplayName());
        verify(chefProfileRepository).save(chef);
    }

    @Test
    void shouldThrowChefNotFoundWhenUpdatingDisplayNameOfNonExistentChef() {
        // Arrange
        when(chefProfileRepository.findById(99L))
                .thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(ChefNotFoundException.class,
                () -> kitchenService.updateDisplayName(99L, "NewName"));

        verify(chefProfileRepository, never()).save(any());
    }

    // ── updateStation ────────────────────────────────────────────────────

    @Test
    void shouldUpdateStationSuccessfully() {
        // Arrange
        ChefProfile chef = new ChefProfile();
        chef.setStation(Station.GRILL);

        when(chefProfileRepository.findById(1L))
                .thenReturn(Optional.of(chef));

        // Act
        kitchenService.updateStation(1L, Station.FRY);

        // Assert
        assertEquals(Station.FRY, chef.getStation());
        verify(chefProfileRepository).save(chef);
    }

    @Test
    void shouldThrowChefNotFoundWhenUpdatingStationOfNonExistentChef() {
        // Arrange
        when(chefProfileRepository.findById(99L))
                .thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(ChefNotFoundException.class,
                () -> kitchenService.updateStation(99L, Station.GRILL));

        verify(chefProfileRepository, never()).save(any());
    }

    // ── updateStatus ─────────────────────────────────────────────────────

    @Test
    void shouldUpdateStatusSuccessfully() {
        // Arrange
        ChefProfile chef = new ChefProfile();
        chef.setStatus(ChefStatus.ACTIVE);

        when(chefProfileRepository.findById(1L))
                .thenReturn(Optional.of(chef));

        // Act
        kitchenService.updateChefStatus(1L, ChefStatus.INACTIVE);

        // Assert
        assertEquals(ChefStatus.INACTIVE, chef.getStatus());
        verify(chefProfileRepository).save(chef);
    }

    @Test
    void shouldThrowChefNotFoundWhenUpdatingStatusOfNonExistentChef() {
        // Arrange
        when(chefProfileRepository.findById(99L))
                .thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(ChefNotFoundException.class,
                () -> kitchenService.updateChefStatus(99L, ChefStatus.INACTIVE));

        verify(chefProfileRepository, never()).save(any());
    }
}
