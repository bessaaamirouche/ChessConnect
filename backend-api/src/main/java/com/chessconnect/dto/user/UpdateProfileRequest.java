package com.chessconnect.dto.user;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDate;
import java.util.List;

@Data
public class UpdateProfileRequest {
    @Size(min = 1, max = 100, message = "First name must be between 1 and 100 characters")
    @Pattern(regexp = "^[\\p{L}\\s\\-']*$", message = "First name contains invalid characters")
    private String firstName;

    @Size(min = 1, max = 100, message = "Last name must be between 1 and 100 characters")
    @Pattern(regexp = "^[\\p{L}\\s\\-']*$", message = "Last name contains invalid characters")
    private String lastName;

    // Teacher fields
    private Integer hourlyRateCents;
    private Boolean acceptsSubscription;
    private String bio;
    private List<String> languages;

    // Teacher banking fields
    private String iban;
    private String bic;
    private String accountHolderName;
    private String siret;
    private String companyName;

    // Student fields
    private LocalDate birthDate;
    private Integer eloRating;
    private Boolean clearEloRating; // Set to true to clear ELO rating

    // Preferences
    private Boolean emailRemindersEnabled;
    private Boolean pushNotificationsEnabled;
}
