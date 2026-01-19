package com.chessconnect.dto.user;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;

@Data
@Builder
public class TeacherProfileResponse {
    private Long id;
    private String email;
    private String firstName;
    private String lastName;
    private String role;

    // Teacher fields
    private Integer hourlyRateCents;
    private Boolean acceptsSubscription;
    private String bio;
    private String avatarUrl;
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

    public static List<String> parseLanguages(String languages) {
        if (languages == null || languages.isBlank()) {
            return List.of();
        }
        return Arrays.asList(languages.split(","));
    }
}
