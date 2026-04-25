package com.restaurant.menu.mapper;

import com.restaurant.menu.dto.IngredientDTO;
import com.restaurant.menu.entity.Ingredient;
import org.mapstruct.Mapper;

import java.util.List;

@Mapper(componentModel = "spring")
public interface IngredientMapper {

    IngredientDTO toDTO(Ingredient entity);

    List<IngredientDTO> toDTOList(List<Ingredient> entities);
}
