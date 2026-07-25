package com.keroles.ewalletddd.pricing.domain.service;

import com.keroles.ewalletddd.pricing.domain.valueObject.FeeCalculationResult;
import com.keroles.ewalletddd.pricing.domain.valueObject.FeeChargeRule;
import com.keroles.ewalletddd.shared.domain.Currency;
import com.keroles.ewalletddd.shared.domain.Money;

final class CashoutFeeCalculationStrategy implements FeeCalculationStrategy {

    @Override
    public FeeCalculationResult calculate(FeeChargeRule rule, Money amount) {
        Currency currency = amount.currency();

        Money senderFeeValue = FeeMath.feeValue(rule.senderFee(), rule.senderFeeType(), amount, currency);
        Money senderVatValue = FeeMath.vatValue(senderFeeValue, rule.vatPercentage(), currency);
        Money receiverFeeValue = Money.zero(currency);
        Money receiverVatValue = Money.zero(currency);

        Money senderTotalAmount = amount.add(senderFeeValue).add(senderVatValue);
        Money receiverTotalAmount = amount.subtract(receiverFeeValue).subtract(receiverVatValue);

        return new FeeCalculationResult(senderTotalAmount, receiverTotalAmount, senderFeeValue, receiverFeeValue, senderVatValue, receiverVatValue);
    }
}
