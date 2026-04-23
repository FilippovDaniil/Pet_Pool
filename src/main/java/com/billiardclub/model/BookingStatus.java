package com.billiardclub.model;

public enum BookingStatus {
    PENDING("Ожидает оплаты"),
    PAID("Оплачено"),
    ACTIVE("Активна"),
    COMPLETED("Завершена"),
    CANCELLED("Отменена");

    private final String displayName;

    BookingStatus(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
