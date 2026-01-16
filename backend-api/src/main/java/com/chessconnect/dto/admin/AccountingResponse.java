package com.chessconnect.dto.admin;

public record AccountingResponse(
        Long totalRevenueCents,
        Long totalCommissionsCents,
        Long totalTeacherEarningsCents,
        Long totalRefundedCents,
        Long totalLessons,
        Long completedLessons,
        Long cancelledLessons
) {}
