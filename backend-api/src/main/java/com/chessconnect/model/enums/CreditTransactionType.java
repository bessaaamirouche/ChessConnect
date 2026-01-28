package com.chessconnect.model.enums;

public enum CreditTransactionType {
    TOPUP,          // Credit top-up via Stripe
    LESSON_PAYMENT, // Payment for a lesson using credit
    REFUND,         // Refund credited back to wallet
    ADMIN_REFUND    // Admin manual refund (wallet cleared for account deletion)
}
