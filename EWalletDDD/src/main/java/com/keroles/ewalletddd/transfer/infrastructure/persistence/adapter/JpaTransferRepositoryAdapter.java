package com.keroles.ewalletddd.transfer.infrastructure.persistence.adapter;

import com.keroles.ewalletddd.transfer.domain.model.Transfer;
import com.keroles.ewalletddd.transfer.domain.repository.TransferRepository;
import com.keroles.ewalletddd.transfer.domain.valueObject.TransferId;
import com.keroles.ewalletddd.transfer.infrastructure.persistence.entity.TransferRequestJpaEntity;
import com.keroles.ewalletddd.transfer.infrastructure.persistence.mapper.TransferMapper;
import com.keroles.ewalletddd.transfer.infrastructure.persistence.repository.SpringDataTransferJpa;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class JpaTransferRepositoryAdapter implements TransferRepository {

    private final SpringDataTransferJpa jpa;

    public JpaTransferRepositoryAdapter(SpringDataTransferJpa jpa) {
        this.jpa = jpa;
    }

    @Override
    public Optional<Transfer> findById(TransferId id) {
        return jpa.findById(id.value()).map(TransferMapper::toDomain);
    }

    @Override
    public void save(Transfer transfer) {
        TransferRequestJpaEntity row = jpa.findById(transfer.id().value())
                .orElseGet(TransferRequestJpaEntity::new);
        TransferMapper.copyOnto(transfer, row);
        jpa.save(row);
    }
}
