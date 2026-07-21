package com.keroles.ewalletddd.pricing.domain.valueObject;

public enum TransactionType {
    TOPUP, TRANSFER, CASHOUT;

    public static TransactionType fromString(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("TransactionType value cannot be null or blank");
        }
        try {
            return TransactionType.valueOf(value.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid TransactionType: " + value, e);
        }
    }
}
