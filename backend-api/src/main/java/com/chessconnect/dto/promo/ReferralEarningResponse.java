package com.chessconnect.dto.promo;

import java.time.LocalDateTime;

public record ReferralEarningResponse(
        Long id,
        Long referredUserId,
        String referredUserName,
        Long lessonId,
        Integer lessonAmountCents,
        Integer platformCommissionCents,
        Integer referrerEarningCents,
        Boolean isPaid,
        LocalDateTime paidAt,
        String paymentReference,
        LocalDateTime createdAt
) {}
