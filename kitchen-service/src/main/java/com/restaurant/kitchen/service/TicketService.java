package com.restaurant.kitchen.service;

import com.restaurant.kitchen.dto.KitchenTicketDTO;
import com.restaurant.kitchen.enums.TicketStatus;
import com.restaurant.kitchen.events.OrderCreatedEvent;

import java.util.List;

public interface TicketService {

    void createKitchenTicket(OrderCreatedEvent event, String correlationId, String sagaId);

    List<KitchenTicketDTO> getActiveTickets();

    KitchenTicketDTO updateTicketStatus(Long id, TicketStatus status);

    void cancelKitchenTicket(Long id, String sagaId, String correlationId);
}
