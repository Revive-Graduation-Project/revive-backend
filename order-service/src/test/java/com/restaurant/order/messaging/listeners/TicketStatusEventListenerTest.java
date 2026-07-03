package com.restaurant.order.messaging.listeners;

import com.restaurant.order.events.TicketStatusUpdatedEvent;
import com.restaurant.order.service.OrderService;
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
        TicketStatusUpdatedEvent event = new TicketStatusUpdatedEvent(1L, 10L, "PREPARING");
        listener.handleTicketStatusUpdated(event);
        verify(orderService).onTicketStarted(10L, 1L);
    }

    @Test
    void handleTicketStatusUpdated_Ready_CallsOnTicketReady() {
        TicketStatusUpdatedEvent event = new TicketStatusUpdatedEvent(1L, 10L, "READY");
        listener.handleTicketStatusUpdated(event);
        verify(orderService).onTicketReady(10L);
    }

    @Test
    void handleTicketStatusUpdated_Done_CallsOnTicketDone() {
        TicketStatusUpdatedEvent event = new TicketStatusUpdatedEvent(1L, 10L, "DONE");
        listener.handleTicketStatusUpdated(event);
        verify(orderService).onTicketDone(10L);
    }

    @Test
    void handleTicketStatusUpdated_Canceled_CallsProcessTicketCancellationSuccess() {
        TicketStatusUpdatedEvent event = new TicketStatusUpdatedEvent(1L, 10L, "CANCELED");
        listener.handleTicketStatusUpdated(event);
        verify(orderService).processTicketCancellationSuccess(10L);
    }
}
