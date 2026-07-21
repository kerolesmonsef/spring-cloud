package com.keroles.ewalletddd.pricing.infrastructure.persistence.adapter;

import com.keroles.ewalletddd.pricing.domain.repository.FeeChargeRepository;
import com.keroles.ewalletddd.pricing.domain.valueObject.FeeChargeRule;
import com.keroles.ewalletddd.pricing.domain.valueObject.TransactionType;
import com.keroles.ewalletddd.pricing.infrastructure.persistence.mapper.FeeChargeMapper;
import com.keroles.ewalletddd.pricing.infrastructure.persistence.repository.SpringDataFeeChargeJpa;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class JpaFeeChargeRepositoryAdapter implements FeeChargeRepository {

    private final SpringDataFeeChargeJpa jpa;

    public JpaFeeChargeRepositoryAdapter(SpringDataFeeChargeJpa jpa) {
        this.jpa = jpa;
    }

    @Override
    @Transactional(readOnly = true)
    public FeeChargeRule findByTransactionType(TransactionType transactionType) {
        return FeeChargeMapper.toDomain(transactionType, jpa.findByTransactionType(transactionType.name()).orElse(null));
    }
}
