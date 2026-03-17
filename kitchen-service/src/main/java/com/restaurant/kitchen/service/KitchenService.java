package com.restaurant.kitchen.service;

import com.restaurant.kitchen.dto.KitchenTicketDTO;

import com.restaurant.kitchen.enums.ChefStatus;
import com.restaurant.kitchen.enums.Station;
import com.restaurant.kitchen.enums.TicketStatus;

import org.springframework.transaction.annotation.Transactional;

import java.util.List;


public interface KitchenService {

    void updateDisplayName(Long id, String displayName);

    void updateStation(Long id, Station station);

    void updateChefStatus(Long id, ChefStatus status);

    @Transactional //both of updating db and publishing event must success
    void updateTicketStatus(Long id, TicketStatus status);

    List<KitchenTicketDTO> getActiveTickets();

}
