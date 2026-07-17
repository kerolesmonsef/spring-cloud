package com.keroles.ewalletddd.accounting.infrastructure.persistence.repository;
  import com.keroles.ewalletddd.accounting.infrastructure.persistence.entity.TransactionJpaEntity;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface SpringDataTransactionJpa extends JpaRepository<TransactionJpaEntity, UUID> {
}
