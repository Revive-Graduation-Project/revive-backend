package com.restaurant.inventory.controller;

import com.restaurant.inventory.dto.IngredientNutrition;
import com.restaurant.inventory.dto.MealNutrition;
import com.restaurant.inventory.dto.NormalizedIngredient;
import com.restaurant.inventory.dto.UsdaFoodDetail;
import com.restaurant.inventory.hooks.OpenRouter;
import com.restaurant.inventory.hooks.UsdaService;
import com.restaurant.inventory.messaging.MenuNutritionPublisher;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
    "spring.datasource.url=jdbc:h2:mem:testdb",
    "spring.datasource.driver-class-name=org.h2.Driver",
    "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
    "spring.jpa.hibernate.ddl-auto=create-drop"
})
@AutoConfigureMockMvc
public class InventoryControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private OpenRouter openRouter;

    @MockBean
    private UsdaService usdaService;

    @MockBean
    private MenuNutritionPublisher menuNutritionPublisher;

    @MockBean
    private com.restaurant.inventory.service.ImportJobService importJobService;

    @Test
    public void testUploadCsvWorkflow() throws Exception {
        // Mock AI response
        String aiResponse = "[{\"originalName\":\"Beef Patty\",\"query\":\"beef\"},{\"originalName\":\"Burger Bun\",\"query\":\"bun\"}]";
        when(openRouter.aiMenuNormalizer(anyString())).thenReturn(aiResponse);

        // Mock USDA API response
        List<UsdaFoodDetail> mockDetails = List.of(
                new UsdaFoodDetail(101, "Beef Patty", "Ground beef", "Protein Foods", List.of()),
                new UsdaFoodDetail(102, "Burger Bun", "Wheat bun", "Baked Products", List.of())
        );
        when(usdaService.fetchNutrients(any(List.class), any(Map.class))).thenReturn(mockDetails);

        // Create a fake CSV file
        String csvContent = "meal_name,category,price,description,ingredients\n" +
                "Cheeseburger,Burger,12.99,\"A juicy beef patty\",\"Beef Patty: 150 g; Burger Bun: 1 piece\"";
        
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "menu.csv",
                "text/csv",
                csvContent.getBytes()
        );

        // Upload CSV and verify the response includes the category
        mockMvc.perform(MockMvcRequestBuilders.multipart("/api/inventory/upload")
                        .file(file)
                        .header("X-User-Role", "ADMIN"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].mealName").value("Cheeseburger"))
                .andExpect(jsonPath("$[0].ingredients[0].ingredientName").value("Beef Patty"))
                .andExpect(jsonPath("$[0].ingredients[0].foodCategory").value("Protein Foods"))
                .andExpect(jsonPath("$[0].ingredients[1].ingredientName").value("Burger Bun"))
                .andExpect(jsonPath("$[0].ingredients[1].foodCategory").value("Baked Products"));
    }

    @Test
    public void testValidateCsvWorkflow() throws Exception {
        String csvContent = "meal_name,category,price,description,ingredients\n" +
                "Cheeseburger,Burger,12.99,\"A juicy beef patty\",\"Beef Patty: 150 g\"\n" +
                ",Burger,10.00,\"Missing name\",\"Beef Patty: 150 g\"\n" +
                "Cheeseburger,Burger,12.99,\"Duplicate\",\"Beef Patty: 150 g\"\n" +
                "NoIngredients,Burger,12.99,\"No ingredients\",\"\"";

        MockMultipartFile file = new MockMultipartFile(
                "file",
                "menu.csv",
                "text/csv",
                csvContent.getBytes()
        );

        mockMvc.perform(MockMvcRequestBuilders.multipart("/api/inventory/validate")
                        .file(file)
                        .header("X-User-Role", "ADMIN"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.validMeals.length()").value(1))
                .andExpect(jsonPath("$.validMeals[0].mealName").value("Cheeseburger"))
                .andExpect(jsonPath("$.invalidMeals.length()").value(3))
                .andExpect(jsonPath("$.invalidMeals[0].reason").value("Missing meal name"))
                .andExpect(jsonPath("$.invalidMeals[1].reason").value("Duplicate meal name in CSV"))
                .andExpect(jsonPath("$.invalidMeals[2].reason").value("No ingredients found"));
    }

    @Test
    public void testGlobalExceptionHandlerRuntime() throws Exception {
        // Trigger a RuntimeException by mocking a failure or sending an invalid file (which throws InvalidCsvException -> handled separately or RuntimeException if in getMenuNutritions)
        // Let's test the RuntimeException thrown by getMenuNutritions when CSV is empty
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "menu.csv",
                "text/csv",
                "".getBytes() // empty file throws InvalidCsvException -> getMenuNutritions wraps it in RuntimeException
        );

        mockMvc.perform(MockMvcRequestBuilders.multipart("/api/inventory/upload")
                        .file(file)
                        .header("X-User-Role", "ADMIN"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.error").value("Internal Server Error"))
                .andExpect(jsonPath("$.message").exists());
    }

    @Test
    public void testImportJsonEndpoint_AdminSuccess() throws Exception {
        when(importJobService.startImport(any(List.class)))
                .thenReturn(Map.of("jobId", "test-job-123", "message", "Import started"));

        String jsonPayload = "[{\"mealName\":\"Burger\",\"category\":\"Main\",\"price\":\"10\",\"description\":\"Desc\",\"ingredients\":[]}]";

        mockMvc.perform(MockMvcRequestBuilders.post("/api/inventory/import-json")
                        .contentType("application/json")
                        .content(jsonPayload)
                        .header("X-User-Role", "ADMIN"))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.jobId").value("test-job-123"));
    }

    @Test
    public void testImportJsonEndpoint_UserForbidden() throws Exception {
        String jsonPayload = "[{}]";

        mockMvc.perform(MockMvcRequestBuilders.post("/api/inventory/import-json")
                        .contentType("application/json")
                        .content(jsonPayload)
                        .header("X-User-Role", "USER"))
                .andExpect(status().isForbidden());
    }

    @Test
    public void testGetActiveJob_ReturnsNoContentWhenEmpty() throws Exception {
        when(importJobService.getActiveJob()).thenReturn(java.util.Optional.empty());

        mockMvc.perform(MockMvcRequestBuilders.get("/api/inventory/import-jobs/active")
                        .header("X-User-Role", "ADMIN"))
                .andExpect(status().isNoContent());
    }

    @Test
    public void testCancelJob_ManagerSuccess() throws Exception {
        com.restaurant.inventory.dto.ImportJobDto mockJob = new com.restaurant.inventory.dto.ImportJobDto(
                "job-1", com.restaurant.inventory.entity.ImportJob.ImportStatus.CANCELED, 10, 5, "Cancelled by admin.", java.time.LocalDateTime.now(), java.time.LocalDateTime.now()
        );
        when(importJobService.cancelJob("job-1")).thenReturn(java.util.Optional.of(mockJob));

        mockMvc.perform(MockMvcRequestBuilders.post("/api/inventory/import-jobs/job-1/cancel")
                        .header("X-User-Role", "MANAGER"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELED"));
    }
}
