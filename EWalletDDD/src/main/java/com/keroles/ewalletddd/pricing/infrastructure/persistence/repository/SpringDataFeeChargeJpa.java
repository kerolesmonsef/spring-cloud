package com.keroles.ewalletddd.pricing.infrastructure.persistence.repository;

import com.keroles.ewalletddd.pricing.infrastructure.persistence.entity.FeeChargeJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SpringDataFeeChargeJpa extends JpaRepository<FeeChargeJpaEntity, Long> {
    boolean existsByTransactionType(String transactionType);
    Optional<FeeChargeJpaEntity> findByTransactionType(String transactionType);
}
