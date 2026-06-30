package com.restaurant.kitchen.messaging;

import com.restaurant.kitchen.events.OrderCanceledEvent;
import com.restaurant.kitchen.events.OrderCreatedEvent;
import com.restaurant.kitchen.events.UserCreatedEvent;
import com.restaurant.kitchen.events.chefProfileEvents.ProfileCreationFailedEvent;
import com.restaurant.kitchen.events.ticketEvents.TicketCanceledFailedEvent;
import com.restaurant.kitchen.events.ticketEvents.TicketCreationFailedEvent;
import com.restaurant.kitchen.service.ChefService;
import com.restaurant.kitchen.service.TicketService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MessageListenerTest {

    @Mock
    private ChefService chefService;

    @Mock
    private TicketService ticketService;

    @Mock
    private MessagePublisher publisher;

    @InjectMocks
    private MessageListener messageListener;

    // ── onUserCreated ───────────────────────────────────────────────────────

    @Test
    void shouldCreateChefProfileOnUserCreated() {
        UserCreatedEvent event = new UserCreatedEvent();
        event.setId(100L);

        messageListener.onUserCreated(event, "corr-001", "saga-001");

        verify(chefService).createChefProfile(event, "corr-001", "saga-001");
    }

    // ── onUserCreatedFailure (DLQ) ─────────────────────────────────────────

    @Test
    void shouldPublishChefFailedEventOnUserCreatedDLQ() {
        UserCreatedEvent event = new UserCreatedEvent();
        event.setId(100L);

        messageListener.onUserCreatedFailure(event, "saga-001", "corr-001");

        verify(publisher).publishChefFailed(any(ProfileCreationFailedEvent.class), eq("saga-001"), eq("corr-001"));
    }

    // ── onOrderCreated ─────────────────────────────────────────────────────

    @Test
    void shouldCreateKitchenTicketOnOrderCreated() {
        OrderCreatedEvent event = new OrderCreatedEvent();
        event.setId(100L);

        messageListener.onOrderCreated(event, "corr-001", "saga-001");

        verify(ticketService).createKitchenTicket(event, "corr-001", "saga-001");
    }

    // ── onOrderCreatedFailure (DLQ) ─────────────────────────────────────────

    @Test
    void shouldPublishTicketFailedEventOnOrderCreatedDLQ() {
        OrderCreatedEvent event = new OrderCreatedEvent();
        event.setId(100L);

        messageListener.onOrderCreatedFailure(event, "saga-001", "corr-001");

        verify(publisher).publishTicketFailed(any(TicketCreationFailedEvent.class), eq("saga-001"), eq("corr-001"));
    }

    // ── onOrderCancelled ───────────────────────────────────────────────────

    @Test
    void shouldCancelKitchenTicketOnOrderCancelled() {
        OrderCanceledEvent event = new OrderCanceledEvent();
        event.setId(100L);

        messageListener.onOrderCancelled(event, "saga-001", "corr-001");

        verify(ticketService).cancelKitchenTicket(100L, "saga-001", "corr-001");
    }

    // ── onOrderCancelledFailure (DLQ) ─────────────────────────────────────

    @Test
    void shouldPublishTicketCanceledFailedEventOnOrderCancelledDLQ() {
        OrderCanceledEvent event = new OrderCanceledEvent();
        event.setId(100L);

        messageListener.onOrderCancelledFailure(event, "saga-001", "corr-001");

        verify(publisher).publishTicketCanceledFailed(any(TicketCanceledFailedEvent.class), eq("saga-001"), eq("corr-001"));
    }
}
