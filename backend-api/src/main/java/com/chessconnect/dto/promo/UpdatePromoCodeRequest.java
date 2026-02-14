package com.chessconnect.dto.promo;

import com.chessconnect.model.enums.DiscountType;

import java.time.LocalDateTime;

public record UpdatePromoCodeRequest(
        DiscountType discountType,
        Double discountPercent,
        String referrerName,
        String referrerEmail,
        Integer premiumDays,
        Double revenueSharePercent,
        Integer maxUses,
        Boolean firstLessonOnly,
        Integer minAmountCents,
        LocalDateTime expiresAt
) {}
