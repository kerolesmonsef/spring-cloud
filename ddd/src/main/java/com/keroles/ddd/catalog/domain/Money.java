package com.keroles.ddd.catalog.domain;

import com.keroles.ddd.sharedkernel.DomainException;
import jakarta.persistence.Embeddable;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

import static lombok.AccessLevel.PROTECTED;

@Embeddable
@Getter
@EqualsAndHashCode
@NoArgsConstructor(access = PROTECTED)
public class Money {

    private BigDecimal amount;

    private Money(BigDecimal amount) {
        this.amount = amount;
    }

    public static Money of(BigDecimal amount) {
        if (amount == null) {
            throw new DomainException("Money amount cannot be null");
        }
        if (amount.signum() < 0) {
            throw new DomainException("Money amount cannot be negative");
        }
        return new Money(amount);
    }

    public static Money of(double amount) {
        return of(BigDecimal.valueOf(amount));
    }

    public boolean isFree() {
        return amount.signum() == 0;
    }

    @Override
    public String toString() {
        return amount.toPlainString();
    }
}
