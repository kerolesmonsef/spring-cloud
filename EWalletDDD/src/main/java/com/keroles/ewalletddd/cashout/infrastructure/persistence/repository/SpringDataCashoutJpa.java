package com.keroles.ewalletddd.cashout.infrastructure.persistence.repository;

import com.keroles.ewalletddd.cashout.infrastructure.persistence.entity.CashoutRequestJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface SpringDataCashoutJpa extends JpaRepository<CashoutRequestJpaEntity, UUID> {
}
