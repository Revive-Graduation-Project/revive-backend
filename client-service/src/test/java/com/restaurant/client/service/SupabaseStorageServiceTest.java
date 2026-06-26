package com.restaurant.client.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestClient;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class SupabaseStorageServiceTest {

    @Mock
    private RestClient restClient;

    @InjectMocks
    private SupabaseStorageService storageService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(storageService, "bucket", "test-bucket");
        ReflectionTestUtils.setField(storageService, "supabaseUrl", "http://test-supabase");
    }

    @Test
    void uploadImage_withEmptyFile_throwsException() {
        MockMultipartFile emptyFile = new MockMultipartFile("file", "test.jpg", "image/jpeg", new byte[0]);
        
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, 
            () -> storageService.uploadImage(emptyFile));
        assertEquals("File cannot be empty or null", exception.getMessage());
    }

    @Test
    void uploadImage_withInvalidContentType_throwsException() {
        MockMultipartFile txtFile = new MockMultipartFile("file", "test.txt", "text/plain", "hello".getBytes());
        
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, 
            () -> storageService.uploadImage(txtFile));
        assertEquals("Invalid file format. Allowed formats are: JPG, JPEG, PNG, WEBP", exception.getMessage());
    }

    @Test
    void uploadImage_withNullContentType_throwsException() {
        MockMultipartFile nullContentFile = new MockMultipartFile("file", "test.jpg", null, "data".getBytes());
        
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, 
            () -> storageService.uploadImage(nullContentFile));
        assertEquals("Invalid file format. Allowed formats are: JPG, JPEG, PNG, WEBP", exception.getMessage());
    }

    // Since RestClient is heavily chained (post().uri()...), mocking it extensively in unit tests
    // can be brittle. We focus on the pre-upload validation edge cases here.
}
