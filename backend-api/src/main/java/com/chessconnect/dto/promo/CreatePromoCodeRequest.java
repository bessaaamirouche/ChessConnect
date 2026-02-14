package com.chessconnect.dto.promo;

import com.chessconnect.model.enums.DiscountType;
import com.chessconnect.model.enums.PromoCodeType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;

public record CreatePromoCodeRequest(
        @NotBlank(message = "Code is required")
        @Size(max = 50, message = "Code must not exceed 50 characters")
        String code,

        @NotNull(message = "Code type is required")
        PromoCodeType codeType,

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
