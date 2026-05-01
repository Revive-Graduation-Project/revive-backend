package com.restaurant.kitchen.service;

import com.restaurant.kitchen.dto.ChefProfileDTO;
import com.restaurant.kitchen.enums.ChefStatus;
import com.restaurant.kitchen.enums.Station;

import com.restaurant.kitchen.events.UserCreatedEvent;


public interface ChefService {

    void createChefProfile(UserCreatedEvent event, String correlationId, String sagaId);

    ChefProfileDTO updateChefDisplayName(Long id, String displayName);

    ChefProfileDTO updateChefStation(Long id, Station station);

    ChefProfileDTO updateChefStatus(Long id, ChefStatus status);

}
