package com.keroles.ewalletddd.topup.infrastructure.persistence.adapter;

import com.keroles.ewalletddd.topup.domain.model.TopupRequest;
import com.keroles.ewalletddd.topup.domain.repository.TopupRepository;
import com.keroles.ewalletddd.topup.domain.valueObject.TopupId;
import com.keroles.ewalletddd.topup.infrastructure.persistence.entity.TopupRequestJpaEntity;
import com.keroles.ewalletddd.topup.infrastructure.persistence.mapper.TopupRequestMapper;
import com.keroles.ewalletddd.topup.infrastructure.persistence.repository.SpringDataTopupJpa;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/** Option B save: load-then-copy onto the managed entity. TopupId is domain-generated, so no id hand-back needed. */
@Component
public class JpaTopupRepositoryAdapter implements TopupRepository {

    private final SpringDataTopupJpa jpa;

    public JpaTopupRepositoryAdapter(SpringDataTopupJpa jpa) {
        this.jpa = jpa;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<TopupRequest> findById(TopupId id) {
        return jpa.findById(id.value()).map(TopupRequestMapper::toDomain);
    }

    @Override
    public void save(TopupRequest topup) {
        TopupRequestJpaEntity row = jpa.findById(topup.id().value())
                .orElseGet(TopupRequestJpaEntity::new);
        TopupRequestMapper.copyOnto(topup, row);
        jpa.save(row);
    }
}
