package com.restaurant.client.dto;

import com.restaurant.client.domain.enums.Gender;
import com.restaurant.client.domain.enums.Goal;
import com.restaurant.client.domain.enums.HealthCondition;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Set;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateClientProfileRequest {
    private Integer age;
    private Gender gender;
    private Boolean exercisesRegularly;
    private Double height;
    private String heightUnit;
    private Double weight;
    private String weightUnit;
    private Goal goal;
    private Set<HealthCondition> healthConditions;
    private String phoneNumber;
}
