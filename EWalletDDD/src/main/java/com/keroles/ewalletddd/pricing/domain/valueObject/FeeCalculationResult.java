package com.keroles.ewalletddd.pricing.domain.valueObject;

import com.keroles.ewalletddd.shared.domain.Money;

public record FeeCalculationResult(Money senderTotalAmount,
                                   Money receiverTotalAmount,
                                   Money senderFeeValue,
                                   Money receiverFeeValue,
                                   Money senderVatValue,
                                   Money receiverVatValue) {
    public Money senderFees() {
        return senderFeeValue.add(senderVatValue);
    }

    public Money receiverFees() {
        return receiverFeeValue.add(receiverVatValue);
    }
}
