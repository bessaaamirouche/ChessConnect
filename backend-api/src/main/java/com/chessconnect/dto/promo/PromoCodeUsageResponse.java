package com.chessconnect.dto.promo;

import java.time.LocalDateTime;

public record PromoCodeUsageResponse(
        Long id,
        Long userId,
        String userName,
        Long lessonId,
        Integer originalAmountCents,
        Integer discountAmountCents,
        Integer commissionSavedCents,
        LocalDateTime usedAt
) {}
