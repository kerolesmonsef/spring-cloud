package com.keroles.ewalletddd.cashout.domain.repository;

import com.keroles.ewalletddd.cashout.domain.model.CashoutRequest;
import com.keroles.ewalletddd.cashout.domain.valueObject.CashoutId;

import java.util.Optional;

public interface CashoutRepository {
    Optional<CashoutRequest> findById(CashoutId id);
    void save(CashoutRequest cashout);
}
