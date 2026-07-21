package com.keroles.ewalletddd.pricing.domain.repository;

import com.keroles.ewalletddd.pricing.domain.valueObject.FeeChargeRule;
import com.keroles.ewalletddd.pricing.domain.valueObject.TransactionType;

public interface FeeChargeRepository {
    FeeChargeRule findByTransactionType(TransactionType transactionType);
}
