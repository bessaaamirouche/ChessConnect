package com.chessconnect.dto.admin;

public record TeacherBalanceListResponse(
        Long teacherId,
        String firstName,
        String lastName,
        String email,
        Integer availableBalanceCents,
        Integer pendingBalanceCents,
        Integer totalEarnedCents,
        Integer totalWithdrawnCents,
        Integer lessonsCompleted
) {}
