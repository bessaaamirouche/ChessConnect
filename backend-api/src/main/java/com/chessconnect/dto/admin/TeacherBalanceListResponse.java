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
        Integer lessonsCompleted,
        // Banking info
        String iban,
        String bic,
        String accountHolderName,
        String siret,
        String companyName,
        // Payout status for current month
        Boolean currentMonthPaid,
        Integer currentMonthEarningsCents,
        Integer currentMonthLessonsCount,
        // Stripe Connect status
        Boolean stripeConnectEnabled,
        Boolean stripeConnectReady
) {}
