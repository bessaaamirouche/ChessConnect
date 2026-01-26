package com.chessconnect.model.enums;

public enum ChessLevel {
    PION(1, "Pion", "Débutant"),
    CAVALIER(2, "Cavalier", "Intermédiaire"),
    FOU(3, "Fou", "Confirmé"),
    TOUR(4, "Tour", "Avancé"),
    DAME(5, "Dame", "Expert"),
    ROI(6, "Roi", "Maître");

    private final int order;
    private final String displayName;
    private final String description;

    ChessLevel(int order, String displayName, String description) {
        this.order = order;
        this.displayName = displayName;
        this.description = description;
    }

    public int getOrder() {
        return order;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getDescription() {
        return description;
    }

    public ChessLevel nextLevel() {
        return switch (this) {
            case PION -> CAVALIER;
            case CAVALIER -> FOU;
            case FOU -> TOUR;
            case TOUR -> DAME;
            case DAME -> ROI;
            case ROI -> ROI;
        };
    }
}
