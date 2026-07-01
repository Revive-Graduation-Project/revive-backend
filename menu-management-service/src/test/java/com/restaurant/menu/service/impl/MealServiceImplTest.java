package com.restaurant.menu.service.impl;

import com.restaurant.menu.entity.Meal;
import com.restaurant.menu.repository.MealRepository;
import com.restaurant.menu.service.SupabaseStorageService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MealServiceImplTest {

    @Mock
    private MealRepository mealRepository;

    @Mock
    private SupabaseStorageService supabaseStorageService;

    @InjectMocks
    private MealServiceImpl mealService;

    @Test
    void uploadBulkMealImages_validFiles_updatesMealsAndSetsActive() {
        MockMultipartFile file1 = new MockMultipartFile("files", "1.jpg", "image/jpeg", "data".getBytes());
        MockMultipartFile file2 = new MockMultipartFile("files", "2.png", "image/png", "data".getBytes());
        MultipartFile[] files = {file1, file2};

        Meal meal1 = Meal.builder().id(1L).isActive(false).build();
        Meal meal2 = Meal.builder().id(2L).isActive(false).imageUrl("old.jpg").build();

        when(mealRepository.findById(1L)).thenReturn(Optional.of(meal1));
        when(mealRepository.findById(2L)).thenReturn(Optional.of(meal2));

        when(supabaseStorageService.uploadImage(file1)).thenReturn("url1.jpg");
        when(supabaseStorageService.uploadImage(file2)).thenReturn("url2.png");

        List<String> urls = mealService.uploadBulkMealImages(files);

        assertEquals(2, urls.size());
        assertTrue(urls.contains("url1.jpg"));
        assertTrue(urls.contains("url2.png"));

        assertTrue(meal1.getIsActive());
        assertEquals("url1.jpg", meal1.getImageUrl());
        
        assertTrue(meal2.getIsActive());
        assertEquals("url2.png", meal2.getImageUrl());
        verify(supabaseStorageService).deleteImage("old.jpg"); // ensure old image deleted

        verify(mealRepository, times(2)).save(any(Meal.class));
    }

    @Test
    void uploadBulkMealImages_invalidFilename_skipsAndContinues() {
        MockMultipartFile validFile = new MockMultipartFile("files", "1.jpg", "image/jpeg", "data".getBytes());
        MockMultipartFile invalidFile = new MockMultipartFile("files", "invalid.jpg", "image/jpeg", "data".getBytes());
        MockMultipartFile noExtFile = new MockMultipartFile("files", "2", "image/jpeg", "data".getBytes());
        
        MultipartFile[] files = {validFile, invalidFile, noExtFile};

        Meal meal1 = Meal.builder().id(1L).isActive(false).build();
        when(mealRepository.findById(1L)).thenReturn(Optional.of(meal1));
        when(supabaseStorageService.uploadImage(validFile)).thenReturn("url1.jpg");

        List<String> urls = mealService.uploadBulkMealImages(files);

        assertEquals(1, urls.size());
        assertEquals("url1.jpg", urls.get(0));
        verify(mealRepository, times(1)).save(meal1);
    }
}
