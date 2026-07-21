package com.keroles.ewalletddd.transfer.domain.repository;

import com.keroles.ewalletddd.transfer.domain.model.Transfer;
import com.keroles.ewalletddd.transfer.domain.valueObject.TransferId;

import java.util.Optional;

public interface TransferRepository {
    Optional<Transfer> findById(TransferId id);
    void save(Transfer transfer);
}
