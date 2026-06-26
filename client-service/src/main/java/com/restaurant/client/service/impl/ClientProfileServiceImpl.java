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
import com.restaurant.client.service.SupabaseStorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ClientProfileServiceImpl implements ClientProfileService {

    private final ClientProfileRepository clientProfileRepository;
    private final PointOperationRepository pointOperationRepository;
    private final SupabaseStorageService supabaseStorageService;

    @Override
    @Transactional
    public void createProfileFromEvent(UserCreatedEvent event) {
        if (clientProfileRepository.existsById(event.getId())) {
            log.info("ClientProfile for user ID: {} already exists. Ignoring duplicate event.", event.getId());
            return;
        }

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
                .loyaltyPoints(0)
                .build();

        clientProfileRepository.save(profile);
        log.info("Successfully created ClientProfile for user ID: {}", event.getId());
    }

    @Override
    public List<ClientProfileDto> getAllProfiles() {
        return clientProfileRepository.findAll()
                .stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    @Override
    public ClientProfileDto getProfile(Long clientId) {
        ClientProfile profile = clientProfileRepository.findById(clientId)
                .orElseThrow(() -> new RuntimeException("Client profile not found"));
        return mapToDto(profile);
    }

    @Override
    @Transactional
    public ClientProfileDto updateProfile(Long clientId, UpdateClientProfileRequest updateClientProfileRequest) {
        log.info("Updating client profile for user id: {}", clientId);
        ClientProfile profile = clientProfileRepository.findById(clientId)
                .orElseThrow(() -> new RuntimeException("Client profile not found"));

        if (updateClientProfileRequest.getAge() != null)
            profile.setAge(updateClientProfileRequest.getAge());
        if (updateClientProfileRequest.getGender() != null)
            profile.setGender(updateClientProfileRequest.getGender());
        if (updateClientProfileRequest.getWeight() != null)
            profile.setWeight(updateClientProfileRequest.getWeight());
        if (updateClientProfileRequest.getHeight() != null)
            profile.setHeight(updateClientProfileRequest.getHeight());
        if (updateClientProfileRequest.getGoal() != null)
            profile.setGoal(updateClientProfileRequest.getGoal());
        if (updateClientProfileRequest.getPhoneNumber() != null)
            profile.setPhoneNumber(updateClientProfileRequest.getPhoneNumber());
        if (updateClientProfileRequest.getHealthConditions() != null) {
            profile.setHealthConditions(updateClientProfileRequest.getHealthConditions());
        }

        ClientProfile saved = clientProfileRepository.save(profile);
        return mapToDto(saved);
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
    public String uploadProfilePicture(Long clientId, MultipartFile file) {
        log.info("Uploading profile picture for client: {}", clientId);
        ClientProfile profile = clientProfileRepository.findById(clientId)
                .orElseGet(() -> clientProfileRepository.save(ClientProfile.builder().id(clientId).build()));

        if (profile.getProfilePictureUrl() != null) {
            supabaseStorageService.deleteImage(profile.getProfilePictureUrl());
        }

        String path = supabaseStorageService.uploadImage(file);
        profile.setProfilePictureUrl(path);
        clientProfileRepository.save(profile);

        return path;
    }

    @Override
    @Transactional
    public void deleteProfilePicture(Long clientId) {
        log.info("Deleting profile picture for client: {}", clientId);
        ClientProfile profile = clientProfileRepository.findById(clientId)
                .orElseThrow(() -> new RuntimeException("Client profile not found"));

        if (profile.getProfilePictureUrl() != null) {
            supabaseStorageService.deleteImage(profile.getProfilePictureUrl());
            profile.setProfilePictureUrl(null);
            clientProfileRepository.save(profile);
        }
    }

    @Override
    @Transactional
    public void redeemPoints(Long clientId, Integer points, Long orderId) {
        if (pointOperationRepository.existsByOrderIdAndOperationType(orderId, PointOperationType.REDEMPTION)) {
            log.info("Point redemption already processed for order ID: {}. Skipping.", orderId);
            return;
        }

        ClientProfile profile = clientProfileRepository.findById(clientId)
                .orElseThrow(() -> new RuntimeException("Client profile not found"));

        if (profile.getLoyaltyPoints() < points) {
            throw new RuntimeException("Insufficient loyalty points");
        }

        profile.setLoyaltyPoints(profile.getLoyaltyPoints() - points);
        clientProfileRepository.save(profile);

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
        if (pointOperationRepository.existsByOrderIdAndOperationType(orderId, PointOperationType.EARNING)) {
            log.info("Point earning already processed for order ID: {}. Skipping.", orderId);
            return;
        }

        ClientProfile profile = clientProfileRepository.findById(clientId)
                .orElseThrow(() -> new RuntimeException("Client profile not found"));

        profile.setLoyaltyPoints(profile.getLoyaltyPoints() + points);
        clientProfileRepository.save(profile);

        pointOperationRepository.save(PointOperation.builder()
                .client(profile)
                .orderId(orderId)
                .operationType(PointOperationType.EARNING)
                .amount(points)
                .build());

        log.info("Added {} points for client {}. New total: {}", points, clientId, profile.getLoyaltyPoints());
    }

    @Override
    @Transactional
    public void rollbackRedemption(Long clientId, Integer points, Long orderId) {
        if (pointOperationRepository.existsByOrderIdAndOperationType(orderId, PointOperationType.ROLLBACK)) {
            log.info("Point rollback already processed for order ID: {}. Skipping.", orderId);
            return;
        }

        ClientProfile profile = clientProfileRepository.findById(clientId)
                .orElseThrow(() -> new RuntimeException("Client profile not found"));

        profile.setLoyaltyPoints(profile.getLoyaltyPoints() + points);
        clientProfileRepository.save(profile);

        pointOperationRepository.save(PointOperation.builder()
                .client(profile)
                .orderId(orderId)
                .operationType(PointOperationType.ROLLBACK)
                .amount(points)
                .build());

        log.info("Rolled back {} points for client {}. New total: {}", points, clientId, profile.getLoyaltyPoints());
    }

    private ClientProfileDto mapToDto(ClientProfile profile) {
        String signedPictureUrl = null;
        if (profile.getProfilePictureUrl() != null) {
            signedPictureUrl = supabaseStorageService.getSignedUrl(profile.getProfilePictureUrl());
        }

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
                .profilePictureUrl(signedPictureUrl)
                .loyaltyPoints(profile.getLoyaltyPoints())
                .build();
    }
}
