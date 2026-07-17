package com.keroles.ewalletddd.accounting.domain.repository;

import com.keroles.ewalletddd.accounting.domain.model.Transaction;

import java.util.Optional;

public interface TransactionRepository {
    Optional<Transaction> findById(Transaction.TransactionId id);
    void save(Transaction transaction);
}
