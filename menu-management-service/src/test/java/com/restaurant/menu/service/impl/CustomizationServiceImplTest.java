package com.restaurant.menu.service.impl;

import com.restaurant.menu.dto.BuildOptionsResponse;
import com.restaurant.menu.dto.IngredientDTO;
import com.restaurant.menu.entity.Ingredient;
import com.restaurant.menu.entity.MealSlot;
import com.restaurant.menu.entity.MealTemplate;
import com.restaurant.menu.mapper.IngredientMapper;
import com.restaurant.menu.repository.IngredientRepository;
import com.restaurant.menu.repository.MealTemplateRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class CustomizationServiceImplTest {

    @Mock
    private MealTemplateRepository mealTemplateRepository;

    @Mock
    private IngredientRepository ingredientRepository;

    @Mock
    private IngredientMapper ingredientMapper;

    @InjectMocks
    private CustomizationServiceImpl customizationService;

    private MealTemplate template;
    private MealSlot slot;
    private Ingredient ingredient;
    private IngredientDTO ingredientDTO;

    @BeforeEach
    void setUp() {
        slot = MealSlot.builder().slotName("Cheese").ingredientCategory("Cheese").isRequired(false).maxSelect(2).build();
        template = MealTemplate.builder().name("Burger Template").primaryCategory("Burger").slots(List.of(slot)).build();
        ingredient = Ingredient.builder().name("Cheddar").category("Cheese").stock(10.0).build();
        ingredientDTO = new IngredientDTO(1L, "Cheddar", "Cheese", "Desc", null, 10.0, 1.0);
    }

    @Test
    void testGetBuildOptions_ReturnsOnlyActiveIngredients() {
        when(mealTemplateRepository.findByPrimaryCategory("Burger")).thenReturn(Optional.of(template));
        when(ingredientRepository.findByCategoryAndStockGreaterThan(eq("Cheese"), anyDouble()))
                .thenReturn(List.of(ingredient));
        when(ingredientMapper.toDTOList(List.of(ingredient))).thenReturn(List.of(ingredientDTO));

        BuildOptionsResponse response = customizationService.getBuildOptions("Burger");

        assertNotNull(response);
        assertEquals("Burger", response.primaryCategory());
        assertEquals(1, response.slots().size());
        assertEquals("Cheese", response.slots().get(0).slotName());
        assertEquals(1, response.slots().get(0).ingredients().size());
        assertEquals("Cheddar", response.slots().get(0).ingredients().get(0).name());
    }
}
