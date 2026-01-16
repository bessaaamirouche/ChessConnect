package com.chessconnect.model.enums;

public enum Language {
    FR("Francais"),
    EN("English"),
    ES("Espanol"),
    DE("Deutsch"),
    IT("Italiano"),
    PT("Portugues"),
    RU("Russkiy"),
    ZH("Zhongwen"),
    AR("Al-Arabiya");

    private final String displayName;

    Language(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
