package com.keroles.ewalletddd.pricing.domain.service;

import com.keroles.ewalletddd.pricing.domain.valueObject.FeeChargeRule;
import com.keroles.ewalletddd.pricing.domain.valueObject.FeeCalculationResult;
import com.keroles.ewalletddd.pricing.domain.valueObject.TransactionType;
import com.keroles.ewalletddd.shared.domain.Money;

public class FeeCalculationService {

    public FeeCalculationResult calculate(FeeChargeRule rule, Money amount) {
        return strategyFor(rule.transactionType()).calculate(rule, amount);
    }

    private FeeCalculationStrategy strategyFor(TransactionType transactionType) {
        return switch (transactionType) {
            case TOPUP -> new TopupFeeCalculationStrategy();
            case CASHOUT -> new CashoutFeeCalculationStrategy();
            case TRANSFER -> new TransferFeeCalculationStrategy();
        };
    }
}
