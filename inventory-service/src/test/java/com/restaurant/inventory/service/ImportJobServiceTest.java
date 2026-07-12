package com.restaurant.inventory.service;

import com.restaurant.inventory.dto.ImportJobDto;
import com.restaurant.inventory.entity.ImportJob;
import com.restaurant.inventory.helper.CsvParserHelper;
import com.restaurant.inventory.repository.ImportJobRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class ImportJobServiceTest {

    @Mock
    private ImportJobRepository importJobRepository;

    @Mock
    private MenuCsvService menuCsvService;

    @InjectMocks
    private ImportJobService importJobService;

    private ImportJob activeJob;
    private ImportJob oldActiveJob;

    @BeforeEach
    void setUp() {
        activeJob = ImportJob.builder()
                .id("job-1")
                .status(ImportJob.ImportStatus.PROCESSING)
                .totalRecords(10)
                .processedRecords(5)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        oldActiveJob = ImportJob.builder()
                .id("job-2")
                .status(ImportJob.ImportStatus.PROCESSING)
                .totalRecords(10)
                .processedRecords(5)
                .createdAt(LocalDateTime.now().minusMinutes(10)) // Older than 5 min
                .updatedAt(LocalDateTime.now().minusMinutes(10))
                .build();
    }

    @Test
    void startImport_ShouldCreateJobAndTriggerAsync() {
        List<CsvParserHelper.MealCsvEntry> meals = List.of(
                new CsvParserHelper.MealCsvEntry("Burger", List.of(), "Main", 10.0, "Desc")
        );

        Map<String, Object> result = importJobService.startImport(meals);

        assertNotNull(result.get("jobId"));
        assertEquals("Import started — 1 meals queued for processing.", result.get("message"));

        verify(importJobRepository, times(1)).save(any(ImportJob.class));
        verify(menuCsvService, times(1)).importFromJsonAsync(eq(meals), anyString());
    }

    @Test
    void getJobStatus_ShouldReturnDto_WhenFound() {
        when(importJobRepository.findById("job-1")).thenReturn(Optional.of(activeJob));

        Optional<ImportJobDto> result = importJobService.getJobStatus("job-1");

        assertTrue(result.isPresent());
        assertEquals("job-1", result.get().id());
        assertEquals(ImportJob.ImportStatus.PROCESSING, result.get().status());
    }

    @Test
    void getActiveJob_ShouldReturnActive_WhenRecent() {
        when(importJobRepository.findActiveJobs()).thenReturn(List.of(activeJob));

        Optional<ImportJobDto> result = importJobService.getActiveJob();

        assertTrue(result.isPresent());
        assertEquals("job-1", result.get().id());
        verify(importJobRepository, never()).save(any(ImportJob.class)); // No timeout save
    }

    @Test
    void getActiveJob_ShouldMarkFailed_WhenTimeoutExceeded() {
        when(importJobRepository.findActiveJobs()).thenReturn(List.of(oldActiveJob));

        Optional<ImportJobDto> result = importJobService.getActiveJob();

        // The job should have been marked as FAILED and saved
        verify(importJobRepository, times(1)).save(oldActiveJob);
        assertEquals(ImportJob.ImportStatus.FAILED, oldActiveJob.getStatus());

        // There should be no active jobs returned
        assertFalse(result.isPresent());
    }

    @Test
    void cancelJob_ShouldMarkCanceled_WhenProcessing() {
        when(importJobRepository.findById("job-1")).thenReturn(Optional.of(activeJob));

        Optional<ImportJobDto> result = importJobService.cancelJob("job-1");

        assertTrue(result.isPresent());
        assertEquals(ImportJob.ImportStatus.CANCELED, result.get().status());
        verify(importJobRepository, times(1)).save(activeJob);
    }
}
