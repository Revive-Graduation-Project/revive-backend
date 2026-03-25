package com.restaurant.kitchen.mapper;

import com.restaurant.kitchen.dto.ChefProfileDTO;
import com.restaurant.kitchen.entity.ChefProfile;
import com.restaurant.kitchen.enums.ChefStatus;
import com.restaurant.kitchen.enums.Station;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mapstruct.factory.Mappers;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

@ExtendWith(MockitoExtension.class)
class ChefProfileMapperTest {

    private final ChefProfileMapper mapper = Mappers.getMapper(ChefProfileMapper.class);

    @Test
    void shouldMapChefProfileToDTO() {
        // Arrange
        ChefProfile chef = ChefProfile.builder()
                .id(1L)
                .authUserId(10L)
                .displayName("John")
                .station(Station.GRILL)
                .status(ChefStatus.ACTIVE)
                .build();

        // Act
        ChefProfileDTO dto = mapper.toDTO(chef);

        // Assert
        Assertions.assertEquals(1L, dto.id());
        Assertions.assertEquals(10L, dto.authUserId());
        Assertions.assertEquals("John", dto.displayName());
        Assertions.assertEquals(Station.GRILL, dto.station());
        Assertions.assertEquals(ChefStatus.ACTIVE, dto.status());
    }

    @Test
    void shouldMapChefProfileListToDTOList() {
        // Arrange
        ChefProfile chef1 = ChefProfile.builder()
                .id(1L)
                .displayName("John")
                .build();
        ChefProfile chef2 = ChefProfile.builder()
                .id(2L)
                .displayName("Jane")
                .build();

        // Act
        List<ChefProfileDTO> dtos = mapper.toDTOList(List.of(chef1, chef2));

        // Assert
        Assertions.assertEquals(2, dtos.size());
        Assertions.assertEquals(1L, dtos.get(0).id());
        Assertions.assertEquals("John", dtos.get(0).displayName());
        Assertions.assertEquals(2L, dtos.get(1).id());
        Assertions.assertEquals("Jane", dtos.get(1).displayName());
    }
}
