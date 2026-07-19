package com.keroles.ewalletddd.accounting.infrastructure.reference;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CurrencyRepository extends JpaRepository<CurrencyJpaEntity, Long> {
    boolean existsByCode(String code);
    Optional<CurrencyJpaEntity> findByCode(String code);
}
