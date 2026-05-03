package com.restaurant.client.service;

import com.restaurant.client.domain.entity.ClientProfile;
import com.restaurant.client.domain.entity.PointOperation;
import com.restaurant.client.domain.enums.Gender;
import com.restaurant.client.domain.enums.Goal;
import com.restaurant.client.domain.enums.HealthCondition;
import com.restaurant.client.domain.enums.PointOperationType;
import com.restaurant.client.dto.ClientProfileDto;
import com.restaurant.client.dto.UpdateClientProfileRequest;
import com.restaurant.client.event.UserCreatedEvent;
import com.restaurant.client.repository.ClientProfileRepository;
import com.restaurant.client.repository.PointOperationRepository;
import com.restaurant.client.service.impl.ClientProfileServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ClientProfileServiceTests {

    @Mock
    private ClientProfileRepository clientProfileRepository;

    @Mock
    private PointOperationRepository pointOperationRepository;

    @InjectMocks
    private ClientProfileServiceImpl clientProfileService;

    private ClientProfile profile;
    private final Long clientId = 1L;
    private final Long orderId = 100L;

    @BeforeEach
    void setUp() {
        profile = ClientProfile.builder()
                .id(clientId)
                .loyaltyPoints(100)
                .build();
    }

    @Test
    void redeemPoints_Success() {
        // Arrange
        when(pointOperationRepository.existsByOrderIdAndOperationType(orderId, PointOperationType.REDEMPTION))
                .thenReturn(false);
        when(clientProfileRepository.findById(clientId)).thenReturn(Optional.of(profile));

        // Act
        clientProfileService.redeemPoints(clientId, 40, orderId);

        // Assert
        assertEquals(60, profile.getLoyaltyPoints());
        verify(clientProfileRepository, times(1)).save(profile);
        verify(pointOperationRepository, times(1)).save(any(PointOperation.class));
    }

    @Test
    void redeemPoints_Idempotency() {
        // Arrange
        when(pointOperationRepository.existsByOrderIdAndOperationType(orderId, PointOperationType.REDEMPTION))
                .thenReturn(true);

        // Act
        clientProfileService.redeemPoints(clientId, 40, orderId);

        // Assert
        assertEquals(100, profile.getLoyaltyPoints()); // Should not change
        verify(clientProfileRepository, never()).save(any());
        verify(pointOperationRepository, never()).save(any());
    }

    @Test
    void redeemPoints_InsufficientPoints() {
        // Arrange
        when(pointOperationRepository.existsByOrderIdAndOperationType(orderId, PointOperationType.REDEMPTION))
                .thenReturn(false);
        when(clientProfileRepository.findById(clientId)).thenReturn(Optional.of(profile));

        // Act & Assert
        assertThrows(RuntimeException.class, () -> 
                clientProfileService.redeemPoints(clientId, 150, orderId));
        
        assertEquals(100, profile.getLoyaltyPoints());
        verify(clientProfileRepository, never()).save(any());
    }

    @Test
    void addPoints_Success() {
        // Arrange
        when(pointOperationRepository.existsByOrderIdAndOperationType(orderId, PointOperationType.EARNING))
                .thenReturn(false);
        when(clientProfileRepository.findById(clientId)).thenReturn(Optional.of(profile));

        // Act
        clientProfileService.addPoints(clientId, 50, orderId);

        // Assert
        assertEquals(150, profile.getLoyaltyPoints());
        verify(clientProfileRepository, times(1)).save(profile);
        verify(pointOperationRepository, times(1)).save(any(PointOperation.class));
    }

    @Test
    void addPoints_Idempotency() {
        // Arrange
        when(pointOperationRepository.existsByOrderIdAndOperationType(orderId, PointOperationType.EARNING))
                .thenReturn(true);

        // Act
        clientProfileService.addPoints(clientId, 50, orderId);

        // Assert
        assertEquals(100, profile.getLoyaltyPoints());
        verify(clientProfileRepository, never()).save(any());
    }

    // --- Profile Management Tests ---

    @Test
    void createProfileFromEvent_Success() {
        // Arrange
        UserCreatedEvent event = UserCreatedEvent.builder()
                .id(2L)
                .role("CLIENT")
                .phoneNumber("123456789")
                .age(25)
                .gender("MALE")
                .goal("LOSE_WEIGHT")
                .healthConditions(List.of("DIABETES"))
                .build();

        // Act
        clientProfileService.createProfileFromEvent(event);

        // Assert
        ArgumentCaptor<ClientProfile> captor = ArgumentCaptor.forClass(ClientProfile.class);
        verify(clientProfileRepository).save(captor.capture());
        
        ClientProfile savedProfile = captor.getValue();
        assertEquals(2L, savedProfile.getId());
        assertEquals("123456789", savedProfile.getPhoneNumber());
        assertEquals(Gender.MALE, savedProfile.getGender());
        assertEquals(Goal.LOSE_WEIGHT, savedProfile.getGoal());
        assertTrue(savedProfile.getHealthConditions().contains(HealthCondition.DIABETES));
        assertEquals(0, savedProfile.getLoyaltyPoints());
    }

    @Test
    void getProfile_Success() {
        // Arrange
        when(clientProfileRepository.findById(clientId)).thenReturn(Optional.of(profile));

        // Act
        ClientProfileDto dto = clientProfileService.getProfile(clientId);

        // Assert
        assertNotNull(dto);
        assertEquals(clientId, dto.getId());
        assertEquals(100, dto.getLoyaltyPoints());
    }

    @Test
    void getProfile_NotFound() {
        // Arrange
        when(clientProfileRepository.findById(clientId)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(RuntimeException.class, () -> clientProfileService.getProfile(clientId));
    }

    @Test
    void updateProfile_Success() {
        // Arrange
        when(clientProfileRepository.findById(clientId)).thenReturn(Optional.of(profile));
        when(clientProfileRepository.save(any(ClientProfile.class))).thenReturn(profile);

        UpdateClientProfileRequest request = new UpdateClientProfileRequest();
        request.setAge(30);
        request.setGender(Gender.FEMALE);
        request.setHealthConditions(Set.of(HealthCondition.HIGH_BLOOD_PRESSURE));

        // Act
        ClientProfileDto dto = clientProfileService.updateProfile(clientId, request);

        // Assert
        assertEquals(30, profile.getAge());
        assertEquals(Gender.FEMALE, profile.getGender());
        assertTrue(profile.getHealthConditions().contains(HealthCondition.HIGH_BLOOD_PRESSURE));
        verify(clientProfileRepository).save(profile);
    }

    @Test
    void deleteProfile_Success() {
        // Arrange
        when(clientProfileRepository.existsById(clientId)).thenReturn(true);

        // Act
        clientProfileService.deleteProfile(clientId);

        // Assert
        verify(clientProfileRepository).deleteById(clientId);
    }

    @Test
    void deleteProfile_NotFound() {
        // Arrange
        when(clientProfileRepository.existsById(clientId)).thenReturn(false);

        // Act & Assert
        assertThrows(RuntimeException.class, () -> clientProfileService.deleteProfile(clientId));
    }
}
