package com.keroles.ewalletddd.pricing.domain.service;

import com.keroles.ewalletddd.pricing.domain.valueObject.FeeChargeRule;
import com.keroles.ewalletddd.pricing.domain.valueObject.FeeCalculationResult;
import com.keroles.ewalletddd.pricing.domain.valueObject.FeeType;
import com.keroles.ewalletddd.pricing.domain.valueObject.TransactionType;
import com.keroles.ewalletddd.shared.domain.Currency;
import com.keroles.ewalletddd.shared.domain.Money;

import java.math.BigDecimal;
import java.math.RoundingMode;

public class FeeCalculationService {

    private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);

    public FeeCalculationResult calculate(FeeChargeRule rule, Money amount) {
        Currency currency = amount.currency();
        TransactionType transactionType = rule.transactionType();

        Money senderFeeValue = transactionType == TransactionType.TOPUP ? Money.zero(currency) : feeValue(rule.senderFee(), rule.senderFeeType(), amount, currency);
        Money receiverFeeValue = transactionType == TransactionType.CASHOUT ? Money.zero(currency) : feeValue(rule.receiverFee(), rule.receiverFeeType(), amount, currency);
        Money senderVatValue = vatValue(senderFeeValue, rule.vatPercentage(), currency);
        Money receiverVatValue = vatValue(receiverFeeValue, rule.vatPercentage(), currency);

        Money senderTotalAmount = amount.add(senderFeeValue).add(senderVatValue);
        Money receiverTotalAmount = amount.subtract(receiverFeeValue).subtract(receiverVatValue);

        return new FeeCalculationResult(senderTotalAmount, receiverTotalAmount, senderFeeValue, receiverFeeValue, senderVatValue, receiverVatValue);
    }

    private Money feeValue(BigDecimal fee, FeeType feeType, Money amount, Currency currency) {
        BigDecimal value = switch (feeType) {
            case VALUE -> fee;
            case PERCENTAGE -> amount.amount().multiply(fee).divide(HUNDRED, 2, RoundingMode.HALF_UP);
        };
        return new Money(value.setScale(2, RoundingMode.HALF_UP), currency);
    }

    private Money vatValue(Money feeValue, BigDecimal vatPercentage, Currency currency) {
        BigDecimal value = feeValue.amount().multiply(vatPercentage).divide(HUNDRED, 2, RoundingMode.HALF_UP);
        return new Money(value, currency);
    }
}
