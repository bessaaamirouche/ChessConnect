package com.chessconnect.model.enums;

import java.util.List;

public enum SubscriptionPlan {
    BASIC(6900, 3, "Basic"),
    STANDARD(12900, 6, "Standard"),
    PREMIUM(17900, 9, "Premium");

    private final int priceCents;
    private final int monthlyQuota;
    private final String displayName;

    SubscriptionPlan(int priceCents, int monthlyQuota, String displayName) {
        this.priceCents = priceCents;
        this.monthlyQuota = monthlyQuota;
        this.displayName = displayName;
    }

    public int getPriceCents() {
        return priceCents;
    }

    public int getMonthlyQuota() {
        return monthlyQuota;
    }

    public String getDisplayName() {
        return displayName;
    }

    public List<String> getFeatures() {
        return switch (this) {
            case BASIC -> List.of(
                    "3 cours par mois",
                    "Accès au cursus complet",
                    "Suivi de progression",
                    "Support par email"
            );
            case STANDARD -> List.of(
                    "6 cours par mois",
                    "Accès au cursus complet",
                    "Suivi de progression",
                    "Support prioritaire",
                    "Replays des sessions"
            );
            case PREMIUM -> List.of(
                    "9 cours par mois",
                    "Accès au cursus complet",
                    "Suivi de progression",
                    "Support VIP 24/7",
                    "Replays des sessions",
                    "Analyse de parties personnalisée"
            );
        };
    }
}
