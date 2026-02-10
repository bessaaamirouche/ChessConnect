package com.chessconnect.service;

import com.chessconnect.model.Lesson;

/**
 * Pricing calculator for group lessons.
 *
 * Pricing model:
 * - Group of 2: each pays 60% of teacher rate (teacher gets 120%)
 * - Group of 3: each pays 45% of teacher rate (teacher gets 135%)
 */
public final class GroupPricingCalculator {

    private static final int PERCENT_FOR_2 = 60;
    private static final int PERCENT_FOR_3 = 45;

    private GroupPricingCalculator() {}

    /**
     * Calculate what each participant pays.
     * @param teacherRateCents teacher's full hourly rate
     * @param targetGroupSize 2 or 3
     * @return price per participant in cents
     */
    public static int calculateParticipantPrice(int teacherRateCents, int targetGroupSize) {
        int percent = switch (targetGroupSize) {
            case 2 -> PERCENT_FOR_2;
            case 3 -> PERCENT_FOR_3;
            default -> throw new IllegalArgumentException("Group size must be 2 or 3, got: " + targetGroupSize);
        };
        return (teacherRateCents * percent) / 100;
    }

    /**
     * Calculate total collected from all participants.
     */
    public static int calculateTotalCollected(int teacherRateCents, int targetGroupSize) {
        return calculateParticipantPrice(teacherRateCents, targetGroupSize) * targetGroupSize;
    }

    /**
     * Calculate platform commission on a given amount (12.5%).
     */
    public static int calculateCommission(int amountCents) {
        return (amountCents * Lesson.COMMISSION_RATE_NUMERATOR) / Lesson.COMMISSION_RATE_DENOMINATOR;
    }

    /**
     * Calculate teacher earnings (amount minus commission).
     */
    public static int calculateTeacherEarnings(int amountCents) {
        return amountCents - calculateCommission(amountCents);
    }

    /**
     * Calculate the savings percentage compared to a private lesson.
     */
    public static int savingsPercent(int targetGroupSize) {
        return switch (targetGroupSize) {
            case 2 -> 40;
            case 3 -> 55;
            default -> 0;
        };
    }
}
