package com.chessconnect.dto.admin;

import com.chessconnect.model.User;

import java.time.LocalDateTime;

public record UserListResponse(
        Long id,
        String email,
        String firstName,
        String lastName,
        String role,
        Boolean isSuspended,
        LocalDateTime createdAt,
        // Teacher-specific
        Integer hourlyRateCents,
        String languages,
        Double averageRating,
        Long reviewCount,
        // Stats
        Long lessonsCount
) {
    public static UserListResponse from(User user, Long lessonsCount, Double averageRating, Long reviewCount) {
        return new UserListResponse(
                user.getId(),
                user.getEmail(),
                user.getFirstName(),
                user.getLastName(),
                user.getRole().name(),
                user.getIsSuspended(),
                user.getCreatedAt(),
                user.getHourlyRateCents(),
                user.getLanguages(),
                averageRating,
                reviewCount,
                lessonsCount
        );
    }
}
