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
        // Banking info (masked for security)
        String ibanMasked,
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
) {
    /**
     * Masks an IBAN showing only the last 4 digits
     * Example: FR76XXXXXXXXXXXXXXXXXXXX1234
     */
    public static String maskIban(String iban) {
        if (iban == null || iban.length() < 8) {
            return iban;
        }
        String countryCode = iban.substring(0, 2);
        String lastFour = iban.substring(iban.length() - 4);
        int middleLength = iban.length() - 6;
        return countryCode + "X".repeat(middleLength) + lastFour;
    }
}
