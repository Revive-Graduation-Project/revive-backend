package com.restaurant.client.service.impl;

import com.restaurant.client.domain.entity.ClientProfile;
import com.restaurant.client.domain.entity.PointOperation;
import com.restaurant.client.domain.enums.Gender;
import com.restaurant.client.domain.enums.Goal;
import com.restaurant.client.domain.enums.HealthCondition;
import com.restaurant.client.domain.enums.PointOperationType;
import com.restaurant.client.dto.ClientProfileDto;
import com.restaurant.client.dto.UpdateClientProfileRequest;
import com.restaurant.client.event.UserCreatedEvent;
import com.restaurant.client.repository.ClientProfileRepository;
import com.restaurant.client.repository.PointOperationRepository;
import com.restaurant.client.service.ClientProfileService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ClientProfileServiceImpl implements ClientProfileService {

    private final ClientProfileRepository clientProfileRepository;
    private final PointOperationRepository pointOperationRepository;

    @Override
    @Transactional
    public void createProfileFromEvent(UserCreatedEvent event) {
        // Map the simple string fields back to our Enums safely
        Gender gender = null;
        if (event.getGender() != null && !event.getGender().isBlank()) {
            try {
                gender = Gender.valueOf(event.getGender().toUpperCase());
            } catch (IllegalArgumentException e) {
                log.warn("Invalid gender provided: {}", event.getGender());
            }
        }

        Goal goal = null;
        if (event.getGoal() != null && !event.getGoal().isBlank()) {
            try {
                goal = Goal.valueOf(event.getGoal().toUpperCase());
            } catch (IllegalArgumentException e) {
                log.warn("Invalid goal provided: {}", event.getGoal());
            }
        }

        Set<HealthCondition> healthConditions = new java.util.HashSet<>();
        if (event.getHealthConditions() != null) {
            for (String condition : event.getHealthConditions()) {
                try {
                    healthConditions.add(HealthCondition.valueOf(condition.toUpperCase()));
                } catch (IllegalArgumentException e) {
                    log.warn("Invalid health condition provided: {}", condition);
                }
            }
        }

        ClientProfile profile = ClientProfile.builder()
                .id(event.getId())
                .phoneNumber(event.getPhoneNumber())
                .age(event.getAge())
                .gender(gender)
                .exercisesRegularly(event.getExercisesRegularly())
                .height(event.getHeight())
                .heightUnit(event.getHeightUnit())
                .weight(event.getWeight())
                .weightUnit(event.getWeightUnit())
                .goal(goal)
                .healthConditions(healthConditions)
                .loyaltyPoints(0) // Default starting points
                .build();

        clientProfileRepository.save(profile);
        log.info("Successfully created ClientProfile for user ID: {}", event.getId());
    }

    @Override
    public ClientProfileDto getProfile(Long clientId) {
        ClientProfile profile = clientProfileRepository.findById(clientId)
                .orElseThrow(() -> new RuntimeException("Client profile not found"));
        return mapToDto(profile);
    }

    @Override
    @Transactional
    public ClientProfileDto updateProfile(Long clientId, UpdateClientProfileRequest request) {
        ClientProfile profile = clientProfileRepository.findById(clientId)
                .orElseThrow(() -> new RuntimeException("Client profile not found"));

        if (request.getAge() != null) profile.setAge(request.getAge());
        if (request.getGender() != null) profile.setGender(request.getGender());
        if (request.getExercisesRegularly() != null) profile.setExercisesRegularly(request.getExercisesRegularly());
        if (request.getHeight() != null) profile.setHeight(request.getHeight());
        if (request.getHeightUnit() != null) profile.setHeightUnit(request.getHeightUnit());
        if (request.getWeight() != null) profile.setWeight(request.getWeight());
        if (request.getWeightUnit() != null) profile.setWeightUnit(request.getWeightUnit());
        if (request.getGoal() != null) profile.setGoal(request.getGoal());
        if (request.getHealthConditions() != null) {
            profile.getHealthConditions().clear();
            profile.getHealthConditions().addAll(request.getHealthConditions());
        }
        if (request.getPhoneNumber() != null) profile.setPhoneNumber(request.getPhoneNumber());

        profile = clientProfileRepository.save(profile);
        return mapToDto(profile);
    }

    @Override
    @Transactional
    public void deleteProfile(Long clientId) {
        if (!clientProfileRepository.existsById(clientId)) {
            throw new RuntimeException("Client profile not found");
        }
        clientProfileRepository.deleteById(clientId);
        log.info("Deleted client profile id: {}", clientId);
    }

    @Override
    @Transactional
    public void redeemPoints(Long clientId, Integer points, Long orderId) {
        // Idempotency check
        if (pointOperationRepository.existsByOrderIdAndOperationType(orderId, PointOperationType.REDEMPTION)) {
            log.info("Point redemption already processed for order ID: {}. Skipping.", orderId);
            return;
        }

        ClientProfile profile = clientProfileRepository.findById(clientId)
                .orElseThrow(() -> new RuntimeException("Client profile not found"));

        if (profile.getLoyaltyPoints() < points) {
            throw new RuntimeException("Insufficient loyalty points. Available: " + profile.getLoyaltyPoints() + ", requested: " + points);
        }

        profile.setLoyaltyPoints(profile.getLoyaltyPoints() - points);
        clientProfileRepository.save(profile);

        // Record operation for idempotency
        pointOperationRepository.save(PointOperation.builder()
                .client(profile)
                .orderId(orderId)
                .operationType(PointOperationType.REDEMPTION)
                .amount(points)
                .build());

        log.info("Redeemed {} points for client {}. Remaining: {}", points, clientId, profile.getLoyaltyPoints());
    }

    @Override
    @Transactional
    public void addPoints(Long clientId, Integer points, Long orderId) {
        // Idempotency check
        if (pointOperationRepository.existsByOrderIdAndOperationType(orderId, PointOperationType.EARNING)) {
            log.info("Point earning already processed for order ID: {}. Skipping.", orderId);
            return;
        }

        ClientProfile profile = clientProfileRepository.findById(clientId)
                .orElseThrow(() -> new RuntimeException("Client profile not found"));

        profile.setLoyaltyPoints(profile.getLoyaltyPoints() + points);
        clientProfileRepository.save(profile);

        // Record operation for idempotency
        pointOperationRepository.save(PointOperation.builder()
                .client(profile)
                .orderId(orderId)
                .operationType(PointOperationType.EARNING)
                .amount(points)
                .build());

        log.info("Added {} points for client {}. New total: {}", points, clientId, profile.getLoyaltyPoints());
    }

    private ClientProfileDto mapToDto(ClientProfile profile) {
        return ClientProfileDto.builder()
                .id(profile.getId())
                .age(profile.getAge())
                .gender(profile.getGender())
                .exercisesRegularly(profile.getExercisesRegularly())
                .height(profile.getHeight())
                .heightUnit(profile.getHeightUnit())
                .weight(profile.getWeight())
                .weightUnit(profile.getWeightUnit())
                .goal(profile.getGoal())
                .healthConditions(profile.getHealthConditions())
                .phoneNumber(profile.getPhoneNumber())
                .loyaltyPoints(profile.getLoyaltyPoints())
                .build();
    }
}
