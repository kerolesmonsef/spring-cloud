package com.keroles.ewalletddd.shared.domain;

import java.math.BigDecimal;

public record Money(BigDecimal amount, Currency currency) {

    
    
    private static final int SCALE = 2;

    public Money {
        if (amount == null || currency == null) throw new IllegalArgumentException("amount and currency required");
        if (amount.signum() < 0) throw new IllegalArgumentException("Money cannot be negative: " + amount);
        amount = amount.setScale(SCALE);
    }

    public static Money of(String amount, String currencyCode) {
        return new Money(new BigDecimal(amount), Currency.of(currencyCode));
    }

    public static Money zero(Currency currency) {
        return new Money(BigDecimal.ZERO, currency);
    }

    public Money add(Money other) {
        assertSameCurrency(other);
        return new Money(amount.add(other.amount), currency);
    }

    public Money subtract(Money other) {
        assertSameCurrency(other);
        return new Money(amount.subtract(other.amount), currency); 
    }

    public boolean isLessThan(Money other) {
        assertSameCurrency(other);
        return amount.compareTo(other.amount) < 0;
    }

    private void assertSameCurrency(Money other) {
        if (!currency.equals(other.currency))
            throw new IllegalArgumentException("Currency mismatch: " + currency + " vs " + other.currency);
    }
}
