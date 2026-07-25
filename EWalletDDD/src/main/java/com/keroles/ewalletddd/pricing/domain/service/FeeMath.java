package com.keroles.ewalletddd.pricing.domain.service;

import com.keroles.ewalletddd.pricing.domain.valueObject.FeeType;
import com.keroles.ewalletddd.shared.domain.Currency;
import com.keroles.ewalletddd.shared.domain.Money;

import java.math.BigDecimal;
import java.math.RoundingMode;

final class FeeMath {

    private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);

    private FeeMath() {
    }

    static Money feeValue(BigDecimal fee, FeeType feeType, Money amount, Currency currency) {
        BigDecimal value = switch (feeType) {
            case VALUE -> fee;
            case PERCENTAGE -> amount.amount().multiply(fee).divide(HUNDRED, 2, RoundingMode.HALF_UP);
        };
        return new Money(value.setScale(2, RoundingMode.HALF_UP), currency);
    }

    static Money vatValue(Money feeValue, BigDecimal vatPercentage, Currency currency) {
        BigDecimal value = feeValue.amount().multiply(vatPercentage).divide(HUNDRED, 2, RoundingMode.HALF_UP);
        return new Money(value, currency);
    }
}
