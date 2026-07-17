package com.keroles.ewalletddd.accounting.infrastructure.persistence.repository;
  import com.keroles.ewalletddd.accounting.infrastructure.persistence.entity.UserJpaEntity;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface SpringDataUserJpa extends JpaRepository<UserJpaEntity, UUID> {
}
