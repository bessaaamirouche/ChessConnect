package com.chessconnect.dto.admin;

public record TeacherBalanceListResponse(
        Long teacherId,
        String firstName,
        String lastName,
        String email,
        Long availableBalanceCents,
        Long pendingBalanceCents,
        Long totalEarnedCents,
        Long totalWithdrawnCents,
        Long lessonsCompleted
) {}
