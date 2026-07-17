package com.keroles.ewalletddd.accounting.infrastructure.persistence.repository;
  import com.keroles.ewalletddd.accounting.infrastructure.persistence.entity.AccountJpaEntity;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SpringDataAccountJpa extends JpaRepository<AccountJpaEntity, UUID> {
    Optional<AccountJpaEntity> findByUserIdAndCurrency(UUID userId, String currency);
    List<AccountJpaEntity> findByUserId(UUID userId);
}
