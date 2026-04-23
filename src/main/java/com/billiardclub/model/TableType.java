package com.billiardclub.model;

public enum TableType {
    RUSSIAN("Русский"),
    AMERICAN("Американка");

    private final String displayName;

    TableType(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
