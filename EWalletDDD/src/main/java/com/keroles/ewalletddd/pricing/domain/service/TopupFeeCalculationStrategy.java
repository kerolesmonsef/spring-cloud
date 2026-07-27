package com.keroles.ewalletddd.pricing.domain.service;

import com.keroles.ewalletddd.pricing.domain.valueObject.FeeCalculationResult;
import com.keroles.ewalletddd.pricing.domain.valueObject.FeeChargeRule;
import com.keroles.ewalletddd.shared.domain.Currency;
import com.keroles.ewalletddd.shared.domain.Money;

final class TopupFeeCalculationStrategy implements FeeCalculationStrategy {

    @Override
    public FeeCalculationResult calculate(FeeChargeRule rule, Money amount) {
        Currency currency = amount.currency();

        Money senderFeeValue = Money.zero(currency);
        Money senderVatValue = Money.zero(currency);
        Money receiverFeeValue = FeeMath.feeValue(rule.receiverFee(), rule.receiverFeeType(), amount, currency);
        Money receiverVatValue = FeeMath.vatValue(receiverFeeValue, rule.vatPercentage(), currency);

        Money senderTotalAmount = amount.add(senderFeeValue).add(senderVatValue);
        Money receiverTotalAmount = amount.subtract(receiverFeeValue).subtract(receiverVatValue);

        return new FeeCalculationResult(senderTotalAmount, receiverTotalAmount, amount, senderFeeValue, receiverFeeValue, senderVatValue, receiverVatValue);
    }
}
