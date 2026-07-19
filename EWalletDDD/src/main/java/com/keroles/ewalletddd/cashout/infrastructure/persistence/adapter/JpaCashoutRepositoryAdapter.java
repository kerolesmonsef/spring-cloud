package com.keroles.ewalletddd.cashout.infrastructure.persistence.adapter;

import com.keroles.ewalletddd.cashout.domain.model.CashoutRequest;
import com.keroles.ewalletddd.cashout.domain.repository.CashoutRepository;
import com.keroles.ewalletddd.cashout.domain.valueObject.CashoutId;
import com.keroles.ewalletddd.cashout.infrastructure.persistence.entity.CashoutRequestJpaEntity;
import com.keroles.ewalletddd.cashout.infrastructure.persistence.mapper.CashoutRequestMapper;
import com.keroles.ewalletddd.cashout.infrastructure.persistence.repository.SpringDataCashoutJpa;
import org.springframework.stereotype.Component;

import java.util.Optional;

/** Option B save: load-then-copy onto the managed entity. CashoutId is domain-generated, so no id hand-back needed. */
@Component
public class JpaCashoutRepositoryAdapter implements CashoutRepository {

    private final SpringDataCashoutJpa jpa;

    public JpaCashoutRepositoryAdapter(SpringDataCashoutJpa jpa) {
        this.jpa = jpa;
    }

    @Override
    public Optional<CashoutRequest> findById(CashoutId id) {
        return jpa.findById(id.value()).map(CashoutRequestMapper::toDomain);
    }

    @Override
    public void save(CashoutRequest cashout) {
        CashoutRequestJpaEntity row = jpa.findById(cashout.id().value())
                .orElseGet(CashoutRequestJpaEntity::new);
        CashoutRequestMapper.copyOnto(cashout, row);
        jpa.save(row);
    }
}
