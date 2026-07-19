package com.keroles.ewalletddd.accounting.infrastructure.persistence.repository;
  import com.keroles.ewalletddd.accounting.infrastructure.persistence.entity.AccountJpaEntity;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SpringDataAccountJpa extends JpaRepository<AccountJpaEntity, Long> {
    Optional<AccountJpaEntity> findByUserIdAndCurrency(Long userId, String currency);
    List<AccountJpaEntity> findByUserId(Long userId);
    boolean existsByAccountTypeAndCurrency(String accountType, String currency);
    Optional<AccountJpaEntity> findFirstByAccountType(String accountType);
    long countByAccountType(String accountType);
}
