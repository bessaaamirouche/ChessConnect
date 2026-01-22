package com.chessconnect.model.enums;

public enum InvoiceType {
    LESSON_INVOICE,      // Teacher -> Student (for the lesson service)
    COMMISSION_INVOICE,  // Platform -> Teacher (for platform fees)
    PAYOUT_INVOICE       // Platform -> Teacher (for payout/transfer to teacher)
}
