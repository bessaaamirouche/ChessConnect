package com.chessconnect.dto.user;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;

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

    // Student fields
    private LocalDate birthDate;
    private Integer eloRating;

    // Preferences
    private Boolean emailRemindersEnabled;
}
