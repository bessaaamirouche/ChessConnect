package com.chessconnect.dto.user;

import lombok.Data;

import java.time.LocalDate;
import java.util.List;

@Data
public class UpdateProfileRequest {
    private String firstName;
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

    // Preferences
    private Boolean emailRemindersEnabled;
}
