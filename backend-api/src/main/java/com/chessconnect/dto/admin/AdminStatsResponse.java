package com.chessconnect.dto.admin;

public record AdminStatsResponse(
        Long totalUsers,
        Long totalStudents,
        Long totalTeachers,
        Long activeSubscriptions,
        Long totalLessons,
        Long lessonsThisMonth,
        Long totalRevenueCents,
        Long revenueThisMonthCents
) {}
