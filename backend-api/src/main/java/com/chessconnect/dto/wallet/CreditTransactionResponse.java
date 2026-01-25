package com.chessconnect.dto.wallet;

import com.chessconnect.model.CreditTransaction;
import com.chessconnect.model.enums.CreditTransactionType;

import java.time.LocalDateTime;

public record CreditTransactionResponse(
        Long id,
        CreditTransactionType transactionType,
        Integer amountCents,
        String description,
        Long lessonId,
        LocalDateTime createdAt
) {
    public static CreditTransactionResponse from(CreditTransaction transaction) {
        return new CreditTransactionResponse(
                transaction.getId(),
                transaction.getTransactionType(),
                transaction.getAmountCents(),
                transaction.getDescription(),
                transaction.getLesson() != null ? transaction.getLesson().getId() : null,
                transaction.getCreatedAt()
        );
    }
}
