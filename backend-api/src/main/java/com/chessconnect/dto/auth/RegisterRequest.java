package com.chessconnect.dto.auth;

import com.chessconnect.model.enums.UserRole;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.util.List;

public record RegisterRequest(
        @NotBlank(message = "Email is required")
        @Email(message = "Invalid email format")
        String email,

        @NotBlank(message = "Password is required")
        @Size(min = 8, message = "Password must be at least 8 characters")
        String password,

        @NotBlank(message = "First name is required")
        String firstName,

        @NotBlank(message = "Last name is required")
        String lastName,

        @NotNull(message = "Role is required")
        UserRole role,

        // Teacher fields
        Integer hourlyRateCents,
        Boolean acceptsSubscription,
        String bio,
        List<String> languages,  // Languages spoken by teacher (e.g., ["FR", "EN"])

        // Student fields
        LocalDate birthDate,
        Integer eloRating
) {}
