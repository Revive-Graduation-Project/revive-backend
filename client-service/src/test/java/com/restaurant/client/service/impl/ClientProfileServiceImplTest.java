package com.restaurant.client.service.impl;

import com.restaurant.client.domain.entity.ClientProfile;
import com.restaurant.client.repository.ClientProfileRepository;
import com.restaurant.client.service.SupabaseStorageService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ClientProfileServiceImplTest {

    @Mock
    private ClientProfileRepository clientProfileRepository;

    @Mock
    private SupabaseStorageService supabaseStorageService;

    @InjectMocks
    private ClientProfileServiceImpl clientProfileService;

    @Test
    void uploadProfilePicture_existingProfileWithoutPicture_uploadsAndSaves() {
        Long clientId = 1L;
        MockMultipartFile file = new MockMultipartFile("file", "test.jpg", "image/jpeg", "data".getBytes());
        ClientProfile profile = ClientProfile.builder().id(clientId).build();

        when(clientProfileRepository.findById(clientId)).thenReturn(Optional.of(profile));
        when(supabaseStorageService.uploadImage(file)).thenReturn("new-path.jpg");
        when(clientProfileRepository.save(any(ClientProfile.class))).thenReturn(profile);

        String path = clientProfileService.uploadProfilePicture(clientId, file);

        assertEquals("new-path.jpg", path);
        assertEquals("new-path.jpg", profile.getProfilePictureUrl());
        verify(supabaseStorageService, never()).deleteImage(any());
        verify(clientProfileRepository).save(profile);
    }

    @Test
    void uploadProfilePicture_existingProfileWithPicture_deletesOldAndUploadsNew() {
        Long clientId = 1L;
        MockMultipartFile file = new MockMultipartFile("file", "test.jpg", "image/jpeg", "data".getBytes());
        ClientProfile profile = ClientProfile.builder().id(clientId).profilePictureUrl("old-path.jpg").build();

        when(clientProfileRepository.findById(clientId)).thenReturn(Optional.of(profile));
        when(supabaseStorageService.uploadImage(file)).thenReturn("new-path.jpg");
        when(clientProfileRepository.save(any(ClientProfile.class))).thenReturn(profile);

        String path = clientProfileService.uploadProfilePicture(clientId, file);

        assertEquals("new-path.jpg", path);
        verify(supabaseStorageService).deleteImage("old-path.jpg");
        verify(clientProfileRepository).save(profile);
    }

    @Test
    void uploadProfilePicture_nonExistingProfile_createsProfileAndUploads() {
        Long clientId = 2L;
        MockMultipartFile file = new MockMultipartFile("file", "test.jpg", "image/jpeg", "data".getBytes());
        ClientProfile newProfile = ClientProfile.builder().id(clientId).build();

        when(clientProfileRepository.findById(clientId)).thenReturn(Optional.empty());
        when(clientProfileRepository.save(any(ClientProfile.class))).thenReturn(newProfile);
        when(supabaseStorageService.uploadImage(file)).thenReturn("new-path.jpg");

        String path = clientProfileService.uploadProfilePicture(clientId, file);

        assertEquals("new-path.jpg", path);
        verify(clientProfileRepository, times(2)).save(any(ClientProfile.class)); // Once for creation, once for update
    }
}
