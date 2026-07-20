package com.keroles.ewalletddd.shared.domain;





public record Currency(String code) {

    public Currency {
        if (code == null || code.isBlank()) throw new IllegalArgumentException("currency code required");
        code = code.trim().toUpperCase();
    }

    public static Currency of(String code) {
        return new Currency(code);
    }
}
