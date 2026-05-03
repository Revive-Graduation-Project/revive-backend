package com.restaurant.client.event;

import com.restaurant.client.service.ClientProfileService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PointsEventListenerTests {

    @Mock
    private ClientProfileService clientProfileService;

    @Mock
    private RabbitTemplate rabbitTemplate;

    @InjectMocks
    private PointsEventListener pointsEventListener;

    private final Long clientId = 1L;
    private final Long orderId = 100L;

    @Test
    void handlePointRedemptionRequested_Success() {
        // Arrange
        PointRedemptionRequestedEvent event = PointRedemptionRequestedEvent.builder()
                .clientId(clientId)
                .pointsToRedeem(50)
                .orderId(orderId)
                .build();

        // Act
        pointsEventListener.handlePointRedemptionRequested(event);

        // Assert
        verify(clientProfileService).redeemPoints(clientId, 50, orderId);
        verify(rabbitTemplate).convertAndSend(any(), any(), any(PointRedemptionSucceededEvent.class));
    }

    @Test
    void handlePointRedemptionRequested_Failure() {
        // Arrange
        PointRedemptionRequestedEvent event = PointRedemptionRequestedEvent.builder()
                .clientId(clientId)
                .pointsToRedeem(50)
                .orderId(orderId)
                .build();
        
        doThrow(new RuntimeException("Insufficient points")).when(clientProfileService)
                .redeemPoints(clientId, 50, orderId);

        // Act
        pointsEventListener.handlePointRedemptionRequested(event);

        // Assert
        verify(rabbitTemplate).convertAndSend(any(), any(), any(PointRedemptionFailedEvent.class));
    }

    @Test
    void handlePointsEarned_Success() {
        // Arrange
        PointsEarnedEvent event = PointsEarnedEvent.builder()
                .clientId(clientId)
                .pointsEarned(20)
                .orderId(orderId)
                .build();

        // Act
        pointsEventListener.handlePointsEarned(event);

        // Assert
        verify(clientProfileService).addPoints(clientId, 20, orderId);
    }

    @Test
    void handlePointRedemptionRollback_Success() {
        // Arrange
        PointRedemptionRollbackRequestedEvent event = PointRedemptionRollbackRequestedEvent.builder()
                .clientId(clientId)
                .pointsToRollback(50)
                .orderId(orderId)
                .build();

        // Act
        pointsEventListener.handlePointRedemptionRollback(event);

        // Assert
        verify(clientProfileService).rollbackRedemption(clientId, 50, orderId);
    }
}
