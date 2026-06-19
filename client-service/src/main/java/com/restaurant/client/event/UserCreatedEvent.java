package com.restaurant.client.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserCreatedEvent {
    private Long id;
    private String role;
    
    // Client Profile fields
    private String phoneNumber;
    private Integer age;
    private String gender;
    private Boolean exercisesRegularly;
    private Double height;
    private String heightUnit;
    private Double weight;
    private String weightUnit;
    private String goal;
    private List<String> healthConditions;
}
