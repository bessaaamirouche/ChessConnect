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
                "Reprise automatique - Reprenez où vous vous êtes arrêté",
                "Notifications prioritaires - Alertes créneaux des coachs favoris",
                "Badge Premium - Badge doré visible sur le profil",
                "Entraînement contre myChessBot - Exercez-vous après vos cours"
        );
    }
}
