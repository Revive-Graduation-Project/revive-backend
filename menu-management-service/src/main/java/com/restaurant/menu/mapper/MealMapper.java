package com.restaurant.menu.mapper;

import com.restaurant.menu.dto.MealDTO;
import com.restaurant.menu.entity.Meal;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring", uses = MealIngredientMapper.class)
public interface MealMapper {

    @Mapping(target = "mealIngredients", source = "mealIngredients")
    MealDTO toDTO(Meal entity);

    List<MealDTO> toDTOList(List<Meal> entities);
}
