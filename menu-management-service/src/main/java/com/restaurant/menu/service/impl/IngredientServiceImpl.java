package com.restaurant.menu.service.impl;

import com.restaurant.menu.dto.BulkUpdateStockRequest.StockEntry;
import com.restaurant.menu.dto.IngredientDTO;
import com.restaurant.menu.dto.IngredientNutrition;
import com.restaurant.menu.entity.Ingredient;
import com.restaurant.menu.exception.IngredientNotFoundException;
import com.restaurant.menu.mapper.IngredientMapper;
import com.restaurant.menu.repository.IngredientRepository;
import com.restaurant.menu.service.IngredientService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class IngredientServiceImpl implements IngredientService {

    private final IngredientRepository ingredientRepository;
    private final IngredientMapper ingredientMapper;

    @Override
    @Transactional
    public Ingredient resolveOrSaveIngredient(IngredientNutrition ingDto) {
        return ingredientRepository.findByName(ingDto.ingredientName())
                .orElseGet(() -> {
                    log.info("Creating new ingredient: '{}'", ingDto.ingredientName());
                    List<Map<String, Object>> ingNutrients = ingDto.nutrients().stream()
                            .map(n -> Map.<String, Object>of(
                                    "nutrientName", n.nutrientName(),
                                    "value", n.value(),
                                    "unitName", n.unitName()))
                            .toList();

                    Ingredient newIng = Ingredient.builder()
                            .name(ingDto.ingredientName())
                            .description(ingDto.description())
                            .nutrients(ingNutrients)
                            .build();
                    return ingredientRepository.save(newIng);
                });
    }

    @Override
    @Transactional(readOnly = true)
    public List<IngredientDTO> getAllIngredients() {
        log.info("Fetching all ingredients");
        List<Ingredient> ingredients = ingredientRepository.findAll();
        return ingredientMapper.toDTOList(ingredients);
    }

    @Override
    @Transactional(readOnly = true)
    public IngredientDTO getIngredientById(Long id) {
        log.info("Fetching ingredient with id: {}", id);
        Ingredient ingredient = ingredientRepository.findById(id)
                .orElseThrow(() -> new IngredientNotFoundException(id));
        return ingredientMapper.toDTO(ingredient);
    }

    @Override
    @Transactional
    public IngredientDTO updateStock(Long id, Integer stock) {
        log.info("Updating stock for ingredient id: {} to {}", id, stock);
        Ingredient ingredient = ingredientRepository.findById(id)
                .orElseThrow(() -> new IngredientNotFoundException(id));
        ingredient.setStock(stock);
        ingredientRepository.save(ingredient);
        return ingredientMapper.toDTO(ingredient);
    }

    @Override
    @Transactional
    public List<IngredientDTO> bulkUpdateStock(List<StockEntry> updates) {
        log.info("Bulk updating stock for {} ingredients", updates.size());

        List<Long> ids = updates.stream().map(StockEntry::ingredientId).toList();
        Map<Long, Ingredient> ingredientMap = ingredientRepository.findAllById(ids).stream()
                .collect(Collectors.toMap(Ingredient::getId, i -> i));

        // Validate all IDs exist before applying any changes
        for (Long id : ids) {
            if (!ingredientMap.containsKey(id)) {
                throw new IngredientNotFoundException(id);
            }
        }

        // Apply stock updates
        for (var entry : updates) {
            Ingredient ingredient = ingredientMap.get(entry.ingredientId());
            ingredient.setStock(entry.stock());
        }

        List<Ingredient> saved = ingredientRepository.saveAll(ingredientMap.values().stream().toList());
        return ingredientMapper.toDTOList(saved);
    }
}
