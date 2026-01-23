package com.chessconnect.model.enums;

import java.util.List;

public enum SubscriptionPlan {
    PREMIUM(499, "Premium");

    private final int priceCents;
    private final String displayName;

    SubscriptionPlan(int priceCents, String displayName) {
        this.priceCents = priceCents;
        this.displayName = displayName;
    }

    public int getPriceCents() {
        return priceCents;
    }

    public String getDisplayName() {
        return displayName;
    }

    public List<String> getFeatures() {
        return List.of(
                "Revisionnage des cours - Accès aux enregistrements vidéo",
                "Notifications prioritaires - Alertes créneaux des coachs favoris",
                "Accès prioritaire - Voir les disponibilités 24h avant",
                "Statistiques avancées - Dashboard détaillé de progression",
                "Badge Premium - Badge doré visible sur le profil"
        );
    }
}
