package com.chessconnect.dto.promo;

import com.chessconnect.model.enums.DiscountType;
import com.chessconnect.model.enums.PromoCodeType;

import java.time.LocalDateTime;

public record PromoCodeResponse(
        Long id,
        String code,
        PromoCodeType codeType,
        DiscountType discountType,
        Double discountPercent,
        String referrerName,
        String referrerEmail,
        Integer premiumDays,
        Double revenueSharePercent,
        Integer maxUses,
        Integer currentUses,
        Boolean firstLessonOnly,
        Integer minAmountCents,
        Boolean isActive,
        LocalDateTime expiresAt,
        LocalDateTime createdAt,
        // Stats
        long totalDiscountCents,
        long totalEarningsCents,
        long unpaidEarningsCents
) {}
