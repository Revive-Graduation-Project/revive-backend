package com.restaurant.menu.mapper;

import com.restaurant.menu.dto.MealDTO;
import com.restaurant.menu.entity.Meal;
import org.mapstruct.Mapper;

import java.util.List;

@Mapper(componentModel = "spring", uses = IngredientMapper.class)
public interface MealMapper {

    MealDTO toDTO(Meal entity);

    List<MealDTO> toDTOList(List<Meal> entities);
}
