package com.restaurant.client.service;

import com.restaurant.client.dto.ClientProfileDto;
import com.restaurant.client.dto.UpdateClientProfileRequest;

import com.restaurant.client.event.UserCreatedEvent;

public interface ClientProfileService {
    void createProfileFromEvent(UserCreatedEvent event);
    ClientProfileDto getProfile(Long clientId);
    ClientProfileDto updateProfile(Long clientId, UpdateClientProfileRequest request);
    void deleteProfile(Long clientId);

    void redeemPoints(Long clientId, Integer points, Long orderId);
    void addPoints(Long clientId, Integer points, Long orderId);
    void rollbackRedemption(Long clientId, Integer points, Long orderId);
}
