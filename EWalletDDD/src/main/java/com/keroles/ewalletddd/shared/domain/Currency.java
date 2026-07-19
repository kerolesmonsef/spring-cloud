package com.keroles.ewalletddd.shared.domain;

// Currency identity for the wallet. Deliberately NOT java.util.Currency: that's ISO-4217 fiat-only
// (getInstance("BTC"/"ETH"/"SOL") throws), and "which currencies exist" is a business decision that
// lives in a_currencies, not the JDK's global registry. Validity is enforced at save (the account
// adapter's findByCode against a_currencies), so this VO stays a pure, registry-free code holder.
public record Currency(String code) {

    public Currency {
        if (code == null || code.isBlank()) throw new IllegalArgumentException("currency code required");
        code = code.trim().toUpperCase();
    }

    public static Currency of(String code) {
        return new Currency(code);
    }
}
