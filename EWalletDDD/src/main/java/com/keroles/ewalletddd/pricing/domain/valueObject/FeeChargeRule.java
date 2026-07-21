package com.keroles.ewalletddd.pricing.domain.valueObject;

import java.math.BigDecimal;

public record FeeChargeRule(TransactionType transactionType,
                             BigDecimal senderFee,
                             FeeType senderFeeType,
                             BigDecimal receiverFee,
                             FeeType receiverFeeType,
                             BigDecimal vatPercentage) {

    public FeeChargeRule {
        if (transactionType == null) throw new IllegalArgumentException("transactionType required");
        if (senderFee == null || senderFeeType == null) throw new IllegalArgumentException("sender fee required");
        if (receiverFee == null || receiverFeeType == null) throw new IllegalArgumentException("receiver fee required");
        if (vatPercentage == null) throw new IllegalArgumentException("vatPercentage required");
    }

    public static FeeChargeRule zero(TransactionType transactionType) {
        return new FeeChargeRule(transactionType, BigDecimal.ZERO, FeeType.VALUE, BigDecimal.ZERO, FeeType.VALUE, BigDecimal.ZERO);
    }
}
