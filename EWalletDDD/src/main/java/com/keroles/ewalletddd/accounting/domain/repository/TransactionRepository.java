package com.keroles.ewalletddd.accounting.domain.repository;

import com.keroles.ewalletddd.accounting.domain.model.Transaction;
import com.keroles.ewalletddd.accounting.domain.valueObject.TransactionId;

import java.util.Optional;

public interface TransactionRepository {
    Optional<Transaction> findById(TransactionId id);
    void save(Transaction transaction);
}
