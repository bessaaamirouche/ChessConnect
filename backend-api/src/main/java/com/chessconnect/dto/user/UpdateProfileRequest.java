package com.chessconnect.dto.user;

import lombok.Data;

import java.time.LocalDate;

@Data
public class UpdateProfileRequest {
    private String firstName;
    private String lastName;

    // Teacher fields
    private Integer hourlyRateCents;
    private Boolean acceptsSubscription;
    private String bio;

    // Student fields
    private LocalDate birthDate;
    private Integer eloRating;

    // Preferences
    private Boolean emailRemindersEnabled;
}
