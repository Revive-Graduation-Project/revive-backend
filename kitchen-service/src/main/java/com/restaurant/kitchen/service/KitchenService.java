package com.restaurant.kitchen.service;

import com.restaurant.kitchen.dto.KitchenTicketDTO;

import com.restaurant.kitchen.enums.ChefStatus;
import com.restaurant.kitchen.enums.Station;
import com.restaurant.kitchen.enums.TicketStatus;

import com.restaurant.kitchen.events.UserCreatedEvent;

import java.util.List;


public interface KitchenService {

    void createChefProfile(UserCreatedEvent event, String correlationId, String sagaId);

    void updateDisplayName(Long id, String displayName);

    void updateStation(Long id, Station station);

    void updateChefStatus(Long id, ChefStatus status);

    void updateTicketStatus(Long id, TicketStatus status);

    List<KitchenTicketDTO> getActiveTickets();
}
