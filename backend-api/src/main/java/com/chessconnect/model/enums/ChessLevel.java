package com.chessconnect.model.enums;

public enum ChessLevel {
    A(1, "Pion", "Débutant"),
    B(2, "Cavalier", "Intermédiaire"),
    C(3, "Reine", "Avancé"),
    D(4, "Roi", "Expert");

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
            case A -> B;
            case B -> C;
            case C -> D;
            case D -> D;
        };
    }

    /**
     * Get the programme level code (A, B, C, or D)
     */
    public String getProgrammeLevel() {
        return this.name();
    }

    /**
     * Get the programme level full name
     */
    public String getProgrammeLevelName() {
        return this.displayName;
    }

    /**
     * Get icon for this level (chess piece unicode)
     */
    public String getIcon() {
        return switch (this) {
            case A -> "♟";  // Pion
            case B -> "♞";  // Cavalier
            case C -> "♛";  // Reine
            case D -> "♚";  // Roi
        };
    }
}
