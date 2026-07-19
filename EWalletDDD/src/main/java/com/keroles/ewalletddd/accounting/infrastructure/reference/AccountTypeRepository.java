package com.keroles.ewalletddd.accounting.infrastructure.reference;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AccountTypeRepository extends JpaRepository<AccountTypeJpaEntity, Long> {
    boolean existsByName(String name);
    Optional<AccountTypeJpaEntity> findByName(String name);
}
