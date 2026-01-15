package com.chessconnect.model.enums;

import java.util.List;

public enum SubscriptionPlan {
    BASIC(6900, 4, "Basic"),
    STANDARD(12900, 8, "Standard"),
    PREMIUM(17900, 12, "Premium");

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
                    "4 cours par mois",
                    "Accès au cursus complet",
                    "Suivi de progression",
                    "Support par email"
            );
            case STANDARD -> List.of(
                    "8 cours par mois",
                    "Accès au cursus complet",
                    "Suivi de progression",
                    "Support prioritaire",
                    "Replays des sessions"
            );
            case PREMIUM -> List.of(
                    "12 cours par mois",
                    "Accès au cursus complet",
                    "Suivi de progression",
                    "Support VIP 24/7",
                    "Replays des sessions",
                    "Analyse de parties personnalisée"
            );
        };
    }
}
