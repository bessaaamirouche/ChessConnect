package com.chessconnect.dto.auth;

import com.chessconnect.model.enums.UserRole;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.util.List;

public record RegisterRequest(
        @NotBlank(message = "Email is required")
        @Email(message = "Invalid email format")
        @Size(max = 255, message = "Email must not exceed 255 characters")
        String email,

        @NotBlank(message = "Password is required")
        @Size(min = 8, max = 128, message = "Password must be between 8 and 128 characters")
        @Pattern(
            regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&€#^()\\-_=+\\[\\]{}|;:'\",.<>/\\\\`~])[A-Za-z\\d@$!%*?&€#^()\\-_=+\\[\\]{}|;:'\",.<>/\\\\`~]{8,}$",
            message = "Password must contain at least one uppercase letter, one lowercase letter, one digit, and one special character"
        )
        String password,

        @NotBlank(message = "First name is required")
        @Size(min = 1, max = 100, message = "First name must be between 1 and 100 characters")
        @Pattern(regexp = "^[\\p{L}\\s\\-']+$", message = "First name contains invalid characters")
        String firstName,

        @NotBlank(message = "Last name is required")
        @Size(min = 1, max = 100, message = "Last name must be between 1 and 100 characters")
        @Pattern(regexp = "^[\\p{L}\\s\\-']+$", message = "Last name contains invalid characters")
        String lastName,

        @NotNull(message = "Role is required")
        UserRole role,

        // Teacher fields
        @Min(value = 1000, message = "Hourly rate must be at least 10€")
        @Max(value = 50000, message = "Hourly rate must not exceed 500€")
        Integer hourlyRateCents,
        @Size(max = 2000, message = "Bio must not exceed 2000 characters")
        String bio,
        @Size(max = 10, message = "Maximum 10 languages allowed")
        List<String> languages,  // Languages spoken by teacher (e.g., ["FR", "EN"])

        // Student fields
        LocalDate birthDate,
        @Min(value = 0, message = "ELO rating cannot be negative")
        @Max(value = 3500, message = "ELO rating must not exceed 3500")
        Integer eloRating,
        @Min(value = 1, message = "Starting course ID must be at least 1")
        @Max(value = 100, message = "Starting course ID must not exceed 100")
        Integer startingCourseId,  // Course ID to start from (default: 1)

        // Referral code (optional)
        @Size(max = 50, message = "Referral code must not exceed 50 characters")
        String referralCode
) {}
