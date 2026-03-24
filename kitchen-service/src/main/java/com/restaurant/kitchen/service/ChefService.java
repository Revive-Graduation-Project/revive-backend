package com.restaurant.kitchen.service;

import com.restaurant.kitchen.enums.ChefStatus;
import com.restaurant.kitchen.enums.Station;

import com.restaurant.kitchen.events.UserCreatedEvent;


public interface ChefService {

    void createChefProfile(UserCreatedEvent event, String correlationId, String sagaId);

    void updateChefDisplayName(Long id, String displayName);

    void updateChefStation(Long id, Station station);

    void updateChefStatus(Long id, ChefStatus status);

}
