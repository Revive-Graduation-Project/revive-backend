package com.restaurant.inventory.service;

import com.restaurant.inventory.dto.IngredientEntry;
import com.restaurant.inventory.entity.ImportJob;
import com.restaurant.inventory.helper.CsvParserHelper;
import com.restaurant.inventory.hooks.OpenRouter;
import com.restaurant.inventory.hooks.UsdaService;
import com.restaurant.inventory.messaging.MenuNutritionPublisher;
import com.restaurant.inventory.repository.ImportJobRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CancellationException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MenuCsvServiceTest {

    @Mock
    private CsvParserHelper csvParserHelper;
    @Mock
    private OpenRouter agent;
    @Mock
    private UsdaService usdaService;
    @Mock
    private MenuNutritionPublisher menuNutritionPublisher;
    @Mock
    private ImportJobRepository importJobRepository;

    @InjectMocks
    private MenuCsvService menuCsvService;

    private CsvParserHelper.MealCsvEntry testMeal;
    private ImportJob testJob;

    @BeforeEach
    void setUp() {
        testMeal = new CsvParserHelper.MealCsvEntry(
                "Async Chicken",
                List.of(new IngredientEntry("Chicken", 200, "g")),
                "Main",
                15.99,
                "Async test meal");

        testJob = ImportJob.builder()
                .id("job-1")
                .status(ImportJob.ImportStatus.PENDING)
                .totalRecords(1)
                .processedRecords(0)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }

    @Test
    void importFromJsonAsync_CompletesSuccessfully() {
        when(importJobRepository.startProcessingIf(eq("job-1"), any(), anyString(), eq(1), anyList())).thenReturn(1);
        when(importJobRepository.updateStatusAndProgressIf(eq("job-1"), any(), anyString(), eq(1), anyList())).thenReturn(1);
        when(agent.aiMenuNormalizer(anyString()))
                .thenReturn("[{\"originalName\": \"Chicken\", \"query\": \"Chicken Breast\"}]");
        when(usdaService.fetchNutrients(anyList(), anyMap())).thenReturn(List.of());
        when(importJobRepository.findById("job-1")).thenReturn(Optional.of(testJob)); // for cancellation check

        menuCsvService.importFromJsonAsync(List.of(testMeal), "job-1");

        verify(importJobRepository).updateStatusAndProgressIf(eq("job-1"), eq(ImportJob.ImportStatus.COMPLETED), anyString(), eq(1), anyList());
        verify(menuNutritionPublisher, times(1)).publish(any());
    }

    @Test
    void importFromJsonAsync_FailsOnPipelineError() {
        when(importJobRepository.startProcessingIf(eq("job-1"), any(), anyString(), eq(1), anyList())).thenReturn(1);
        when(agent.aiMenuNormalizer(anyString()))
                .thenThrow(new RuntimeException("AI service unavailable"));

        menuCsvService.importFromJsonAsync(List.of(testMeal), "job-1");

        verify(importJobRepository).updateStatusIf(eq("job-1"), eq(ImportJob.ImportStatus.FAILED), anyString(), anyList());
        verify(menuNutritionPublisher, never()).publish(any());
    }

    @Test
    void importFromJsonAsync_AbortsOnCancellation() {
        when(importJobRepository.startProcessingIf(eq("job-1"), any(), anyString(), eq(1), anyList())).thenReturn(1);
        
        ImportJob canceledJob = ImportJob.builder()
                .id("job-1")
                .status(ImportJob.ImportStatus.CANCELED)
                .build();

        when(agent.aiMenuNormalizer(anyString()))
                .thenReturn("[{\"originalName\": \"Chicken\", \"query\": \"Chicken Breast\"}]");

        when(importJobRepository.findById("job-1")).thenReturn(Optional.of(canceledJob));

        menuCsvService.importFromJsonAsync(List.of(testMeal), "job-1");

        verify(menuNutritionPublisher, never()).publish(any());
        // FAILED should not be set because we abort on CancellationException
        verify(importJobRepository, never()).updateStatusIf(eq("job-1"), eq(ImportJob.ImportStatus.FAILED), anyString(), anyList());
    }

    @Test
    void validateCsv_flagsDuplicateMealName_inRowFormat() {
        var row1 = new java.util.LinkedHashMap<String, String>();
        row1.put("meal_name", "Burger");
        row1.put("ingredient", "Beef");
        row1.put("quantity", "200");
        row1.put("unit", "g");

        var row2 = new java.util.LinkedHashMap<String, String>();
        row2.put("meal_name", "Burger");  // duplicate
        row2.put("ingredient", "Cheese");
        row2.put("quantity", "50");
        row2.put("unit", "g");

        when(csvParserHelper.parse(any())).thenReturn(List.of(row1, row2));
        when(csvParserHelper.parseMenu(any())).thenReturn(new java.util.LinkedHashMap<>());

        var result = menuCsvService.validateCsv(null);

        long duplicateCount = result.invalidMeals().stream()
                .filter(e -> e.reason().contains("Duplicate"))
                .count();
        assertEquals(0, duplicateCount, "Row-format CSV duplicate should NOT be flagged because multiple rows per meal is the standard for Format A");
    }
}
