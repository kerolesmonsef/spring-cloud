package com.keroles.ewalletddd.transfer.infrastructure.persistence.repository;

import com.keroles.ewalletddd.transfer.infrastructure.persistence.entity.TransferRequestJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface SpringDataTransferJpa extends JpaRepository<TransferRequestJpaEntity, UUID> {
}
