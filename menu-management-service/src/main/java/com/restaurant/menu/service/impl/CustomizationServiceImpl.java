package com.restaurant.menu.service.impl;

import com.restaurant.menu.dto.BuildOptionsResponse;
import com.restaurant.menu.dto.BuildOptionsResponse.SlotOptions;
import com.restaurant.menu.dto.IngredientDTO;
import com.restaurant.menu.entity.Ingredient;
import com.restaurant.menu.entity.MealTemplate;
import com.restaurant.menu.exception.TemplateNotFoundException;
import com.restaurant.menu.mapper.IngredientMapper;
import com.restaurant.menu.repository.IngredientRepository;
import com.restaurant.menu.repository.MealTemplateRepository;
import com.restaurant.menu.service.CustomizationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CustomizationServiceImpl implements CustomizationService {

    private final MealTemplateRepository mealTemplateRepository;
    private final IngredientRepository ingredientRepository;
    private final IngredientMapper ingredientMapper;

    @Override
    public BuildOptionsResponse getBuildOptions(String primaryCategory) {
        MealTemplate template = mealTemplateRepository.findByPrimaryCategory(primaryCategory)
                .orElseThrow(() -> new TemplateNotFoundException("No template found for category: " + primaryCategory));

        List<SlotOptions> slotOptions = template.getSlots().stream().map(slot -> {
            List<Ingredient> ingredients = ingredientRepository.findByCategoryAndStockGreaterThan(slot.getIngredientCategory(), 0.0);
            List<IngredientDTO> ingredientDTOs = ingredientMapper.toDTOList(ingredients);
            
            return new SlotOptions(
                    slot.getSlotName(),
                    slot.getIngredientCategory(),
                    slot.getIsRequired(),
                    slot.getMaxSelect(),
                    ingredientDTOs
            );
        }).collect(Collectors.toList());

        return new BuildOptionsResponse(primaryCategory, slotOptions);
    }
}
