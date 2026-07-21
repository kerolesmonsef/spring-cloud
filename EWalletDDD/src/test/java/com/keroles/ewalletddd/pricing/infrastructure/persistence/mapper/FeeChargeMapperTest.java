package com.keroles.ewalletddd.pricing.infrastructure.persistence.mapper;

import com.keroles.ewalletddd.pricing.domain.valueObject.FeeChargeRule;
import com.keroles.ewalletddd.pricing.domain.valueObject.TransactionType;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class FeeChargeMapperTest {

    @Test
    void toDomain_noRowFound_returnsZeroRule() {
        FeeChargeRule result = FeeChargeMapper.toDomain(TransactionType.TOPUP, null);

        assertEquals(FeeChargeRule.zero(TransactionType.TOPUP), result);
    }
}
