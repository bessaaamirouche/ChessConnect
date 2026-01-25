package com.chessconnect.model.enums;

public enum DifficultyLevel {
    DEBUTANT(1, "Debutant", 0, 300),
    FACILE(2, "Facile", 5, 300),
    MOYEN(3, "Moyen", 10, 300),
    DIFFICILE(4, "Difficile", 15, 300),
    EXPERT(5, "Expert", 20, 300);

    private final int order;
    private final String displayName;
    private final int stockfishSkillLevel;
    private final int thinkTimeMs;

    DifficultyLevel(int order, String displayName, int stockfishSkillLevel, int thinkTimeMs) {
        this.order = order;
        this.displayName = displayName;
        this.stockfishSkillLevel = stockfishSkillLevel;
        this.thinkTimeMs = thinkTimeMs;
    }

    public int getOrder() {
        return order;
    }

    public String getDisplayName() {
        return displayName;
    }

    public int getStockfishSkillLevel() {
        return stockfishSkillLevel;
    }

    public int getThinkTimeMs() {
        return thinkTimeMs;
    }
}
