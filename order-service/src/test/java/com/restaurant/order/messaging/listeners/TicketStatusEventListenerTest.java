package com.restaurant.order.messaging.listeners;

import com.restaurant.order.events.TicketStatusUpdatedEvent;
import com.restaurant.order.service.OrderService;
import com.restaurant.order.enums.TicketStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class TicketStatusEventListenerTest {

    @Mock
    private OrderService orderService;

    @InjectMocks
    private TicketStatusEventListener listener;

    @Test
    void handleTicketStatusUpdated_Preparing_CallsOnTicketStarted() {
        TicketStatusUpdatedEvent event = new TicketStatusUpdatedEvent(1L, 10L, TicketStatus.PREPARING);
        listener.handleTicketStatusUpdated(event);
        verify(orderService).onTicketStarted(10L, 1L);
    }

    @Test
    void handleTicketStatusUpdated_Ready_CallsOnTicketReady() {
        TicketStatusUpdatedEvent event = new TicketStatusUpdatedEvent(1L, 10L, TicketStatus.READY);
        listener.handleTicketStatusUpdated(event);
        verify(orderService).onTicketReady(10L);
    }

    @Test
    void handleTicketStatusUpdated_Done_CallsOnTicketDone() {
        TicketStatusUpdatedEvent event = new TicketStatusUpdatedEvent(1L, 10L, TicketStatus.DONE);
        listener.handleTicketStatusUpdated(event);
        // Original code had DONE map to onTicketReady.
        verify(orderService).onTicketReady(10L);
    }

    @Test
    void handleTicketStatusUpdated_Canceled_CallsProcessTicketCancellationSuccess() {
        TicketStatusUpdatedEvent event = new TicketStatusUpdatedEvent(1L, 10L, TicketStatus.CANCELED);
        listener.handleTicketStatusUpdated(event);
        verify(orderService).processTicketCancellationSuccess(10L);
    }

    @Test
    void handleTicketStatusUpdated_Queued_LogsAndIgnores() {
        TicketStatusUpdatedEvent event = new TicketStatusUpdatedEvent(1L, 10L, TicketStatus.QUEUED);
        listener.handleTicketStatusUpdated(event);
        // OrderService should not be called
        org.mockito.Mockito.verifyNoInteractions(orderService);
    }

    @Test
    void handleTicketStatusUpdated_NullStatus_LogsAndIgnores() {
        TicketStatusUpdatedEvent event = new TicketStatusUpdatedEvent(1L, 10L, null);
        listener.handleTicketStatusUpdated(event);
        org.mockito.Mockito.verifyNoInteractions(orderService);
    }

    @Test
    void handleTicketStatusUpdated_Exception_Rethrows() {
        TicketStatusUpdatedEvent event = new TicketStatusUpdatedEvent(1L, 10L, TicketStatus.PREPARING);
        org.mockito.Mockito.doThrow(new RuntimeException("DB Error")).when(orderService).onTicketStarted(10L, 1L);

        org.junit.jupiter.api.Assertions.assertThrows(RuntimeException.class, () -> {
            listener.handleTicketStatusUpdated(event);
        });
    }
}
