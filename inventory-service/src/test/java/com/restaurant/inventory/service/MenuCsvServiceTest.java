package com.restaurant.inventory.service;

import com.restaurant.inventory.dto.ImportJobStatus;
import com.restaurant.inventory.dto.ImportJobStatus.JobState;
import com.restaurant.inventory.dto.ImportResponse;
import com.restaurant.inventory.dto.IngredientEntry;
import com.restaurant.inventory.helper.CsvParserHelper;
import com.restaurant.inventory.hooks.OpenRouter;
import com.restaurant.inventory.hooks.UsdaService;
import com.restaurant.inventory.messaging.MenuNutritionPublisher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
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

    // Use a real ImportJobStore — it has no external dependencies
    @Spy
    private ImportJobStore importJobStore = new ImportJobStore();

    @InjectMocks
    private MenuCsvService menuCsvService;

    private CsvParserHelper.MealCsvEntry testMeal;

    @BeforeEach
    void setUp() {
        testMeal = new CsvParserHelper.MealCsvEntry(
                "Async Chicken",
                List.of(new IngredientEntry("Chicken", 200, "g")),
                "Main",
                15.99,
                "Async test meal");
    }

    // ─── Test 1: importFromJson returns immediately ───────────────────────────

    @Test
    void importFromJson_returnsImmediately_doesNotBlockOnPipeline() throws InterruptedException {
        // Make the AI agent block for 3 seconds — lenient because this is never actually called
        // (the method returns before the background thread reaches the AI call — that's the point)
        CountDownLatch pipelineLatch = new CountDownLatch(1);
        lenient().when(agent.aiMenuNormalizer(anyString())).thenAnswer(inv -> {
            pipelineLatch.await(3, TimeUnit.SECONDS);
            return "[{\"originalName\": \"Chicken\", \"query\": \"Chicken Breast\"}]";
        });

        // Act
        long startTime = System.currentTimeMillis();
        ImportResponse response = menuCsvService.importFromJson(List.of(testMeal));
        long duration = System.currentTimeMillis() - startTime;

        // Assert — must return in under 1 second
        assertTrue(duration < 1000, "importFromJson should return immediately, took " + duration + "ms");
        assertNotNull(response);
        assertNotNull(response.jobId(), "Response must include a jobId for polling");
        assertEquals(1, response.mealCount());

        pipelineLatch.countDown(); // Release the background thread
    }

    // ─── Test 2: job reaches DONE on success ─────────────────────────────────

    @Test
    void importFromJson_marksJobDone_whenPipelineSucceeds() throws InterruptedException {
        // Arrange — pipeline succeeds instantly
        when(agent.aiMenuNormalizer(anyString()))
                .thenReturn("[{\"originalName\": \"Chicken\", \"query\": \"Chicken Breast\"}]");
        when(usdaService.fetchNutrients(anyList(), anyMap())).thenReturn(List.of());

        // Act
        ImportResponse response = menuCsvService.importFromJson(List.of(testMeal));
        String jobId = response.jobId();

        // Wait for background thread to complete (up to 5s)
        long deadline = System.currentTimeMillis() + 5000;
        ImportJobStatus status;
        do {
            Thread.sleep(50);
            status = importJobStore.getStatus(jobId).orElseThrow();
        } while (!status.isTerminal() && System.currentTimeMillis() < deadline);

        // Assert
        assertEquals(JobState.DONE, status.state(), "Job should reach DONE state after successful pipeline");
        assertNull(status.errorMessage());
    }

    // ─── Test 3: job reaches FAILED on pipeline error ────────────────────────

    @Test
    void importFromJson_marksJobFailed_whenPipelineThrows() throws InterruptedException {
        // Arrange — AI normalizer throws to simulate Step 3 failure
        when(agent.aiMenuNormalizer(anyString()))
                .thenThrow(new RuntimeException("AI service unavailable"));

        // Act
        ImportResponse response = menuCsvService.importFromJson(List.of(testMeal));
        String jobId = response.jobId();

        // Wait for background thread to complete (up to 5s)
        long deadline = System.currentTimeMillis() + 5000;
        ImportJobStatus status;
        do {
            Thread.sleep(50);
            status = importJobStore.getStatus(jobId).orElseThrow();
        } while (!status.isTerminal() && System.currentTimeMillis() < deadline);

        // Assert
        assertEquals(JobState.FAILED, status.state(), "Job should reach FAILED state when pipeline throws");
        assertNotNull(status.errorMessage(), "Error message must be recorded for frontend display");
        assertTrue(status.errorMessage().contains("AI service unavailable"));
    }

    // ─── Test 4: duplicate check applies to both CSV formats ─────────────────

    @Test
    void validateCsv_flagsDuplicateMealName_inRowFormat() {
        // Row-format CSV has columns: meal_name, ingredient, quantity, unit
        // (no "ingredients" column — so isListFormat=false)
        // Two rows for the same meal name should produce a duplicate InvalidMealEntry

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

        // Only row-format rows — no "ingredients" key → isListFormat = false
        when(csvParserHelper.parse(any())).thenReturn(List.of(row1, row2));
        when(csvParserHelper.parseMenu(any())).thenReturn(new java.util.LinkedHashMap<>());

        var result = menuCsvService.validateCsv(null);

        long duplicateCount = result.invalidMeals().stream()
                .filter(e -> e.reason().contains("Duplicate"))
                .count();
        assertEquals(1, duplicateCount, "Row-format CSV duplicate should be flagged even when isListFormat=false");
    }
}
