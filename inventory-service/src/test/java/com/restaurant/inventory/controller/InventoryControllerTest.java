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

@SpringBootTest
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
}
