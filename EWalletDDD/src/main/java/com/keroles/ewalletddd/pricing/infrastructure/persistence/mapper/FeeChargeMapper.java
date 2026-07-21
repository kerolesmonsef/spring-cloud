package com.keroles.ewalletddd.pricing.infrastructure.persistence.mapper;

import com.keroles.ewalletddd.pricing.domain.valueObject.FeeChargeRule;
import com.keroles.ewalletddd.pricing.domain.valueObject.FeeType;
import com.keroles.ewalletddd.pricing.domain.valueObject.TransactionType;
import com.keroles.ewalletddd.pricing.infrastructure.persistence.entity.FeeChargeJpaEntity;

public final class FeeChargeMapper {

    private FeeChargeMapper() {}

    public static FeeChargeRule toDomain(TransactionType transactionType, FeeChargeJpaEntity row) {
        if (row == null) {
            return FeeChargeRule.zero(transactionType);
        }
        return new FeeChargeRule(
                TransactionType.valueOf(row.getTransactionType()),
                row.getSenderFee(),
                FeeType.valueOf(row.getSenderFeeType()),
                row.getReceiverFee(),
                FeeType.valueOf(row.getReceiverFeeType()),
                row.getVatPercentage());
    }
}
