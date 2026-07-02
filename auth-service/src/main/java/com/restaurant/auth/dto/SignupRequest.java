package com.restaurant.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record SignupRequest(

        @NotBlank(message = "Email must not be blank")
        @Email(message = "Invalid email format")
        String email,

        @NotBlank(message = "Password must not be blank")
        @Size(min = 6, message = "Password must be at least 6 characters")
        String password,

        @NotBlank(message = "First name must not be blank")
        String firstName,

        @NotBlank(message = "Last name must not be blank")
        String lastName,

        // Client Profile fields
        String phoneNumber,
        Integer age,
        String gender,
        Boolean exercisesRegularly,
        Double height,
        String heightUnit,
        Double weight,
        String weightUnit,
        String goal,
        java.util.List<String> healthConditions

) {}
