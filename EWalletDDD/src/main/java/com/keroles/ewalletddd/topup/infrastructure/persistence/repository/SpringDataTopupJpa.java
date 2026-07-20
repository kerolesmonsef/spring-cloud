package com.keroles.ewalletddd.topup.infrastructure.persistence.repository;

import com.keroles.ewalletddd.topup.infrastructure.persistence.entity.TopupRequestJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface SpringDataTopupJpa extends JpaRepository<TopupRequestJpaEntity, UUID> {
}
