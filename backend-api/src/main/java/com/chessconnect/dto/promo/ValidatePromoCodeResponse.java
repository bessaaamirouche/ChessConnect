package com.chessconnect.dto.promo;

import com.chessconnect.model.enums.DiscountType;

public record ValidatePromoCodeResponse(
        boolean valid,
        String message,
        DiscountType discountType,
        Double discountPercent,
        Integer finalPriceCents,
        Integer discountAmountCents
) {}
