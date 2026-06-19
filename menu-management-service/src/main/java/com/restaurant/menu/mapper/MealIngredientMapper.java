package com.restaurant.menu.mapper;

import com.restaurant.menu.dto.MealIngredientDTO;
import com.restaurant.menu.entity.MealIngredient;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring", uses = IngredientMapper.class)
public interface MealIngredientMapper {

    @Mapping(target = "ingredient", source = "ingredient")
    MealIngredientDTO toDTO(MealIngredient entity);

    List<MealIngredientDTO> toDTOList(List<MealIngredient> entities);
}
