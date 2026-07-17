package com.keroles.ewalletddd.accounting.infrastructure.persistence.adapter;
  import com.keroles.ewalletddd.accounting.infrastructure.persistence.entity.TransactionJpaEntity; import com.keroles.ewalletddd.accounting.infrastructure.persistence.mapper.TransactionMapper; import com.keroles.ewalletddd.accounting.infrastructure.persistence.repository.SpringDataTransactionJpa;

import com.keroles.ewalletddd.accounting.domain.model.Transaction;
import com.keroles.ewalletddd.accounting.domain.repository.TransactionRepository;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class JpaTransactionRepositoryAdapter implements TransactionRepository {

    private final SpringDataTransactionJpa jpa;

    public JpaTransactionRepositoryAdapter(SpringDataTransactionJpa jpa) {
        this.jpa = jpa;
    }

    @Override
    public Optional<Transaction> findById(Transaction.TransactionId id) {
        return jpa.findById(id.value()).map(TransactionMapper::toDomain);
    }

    @Override
    public void save(Transaction transaction) {
        TransactionJpaEntity row = jpa.findById(transaction.id().value())
                .orElseGet(TransactionJpaEntity::new);
        TransactionMapper.copyOnto(transaction, row);
        jpa.save(row);
    }
}
