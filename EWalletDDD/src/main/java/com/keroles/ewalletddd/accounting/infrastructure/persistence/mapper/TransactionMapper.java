package com.keroles.ewalletddd.accounting.infrastructure.persistence.mapper;
  import com.keroles.ewalletddd.accounting.infrastructure.persistence.entity.TransactionJpaEntity;

import com.keroles.ewalletddd.accounting.domain.valueObject.AccountId;
import com.keroles.ewalletddd.accounting.domain.valueObject.AccountType;
import com.keroles.ewalletddd.accounting.domain.model.Transaction;
import com.keroles.ewalletddd.accounting.domain.valueObject.Party;
import com.keroles.ewalletddd.accounting.domain.valueObject.TransactionId;
import com.keroles.ewalletddd.shared.domain.Currency;
import com.keroles.ewalletddd.shared.domain.Money;

public final class TransactionMapper {

    private TransactionMapper() {}

    public static Transaction toDomain(TransactionJpaEntity row) {
        return Transaction.restore(
                new TransactionId(row.getId()),
                Transaction.Type.valueOf(row.getType()),
                new Party(row.getSenderReference(), AccountType.valueOf(row.getSenderType())),
                new Party(row.getReceiverReference(), AccountType.valueOf(row.getReceiverType())),
                Transaction.Status.valueOf(row.getStatus()),
                row.getCreatedAt(),
                row.getEntries().stream().map(TransactionMapper::entryToDomain).toList());
    }

    private static Transaction.Entry entryToDomain(TransactionJpaEntity.TransactionEntryJpaEntity row) {
        Currency currency = Currency.of(row.getCurrency());
        return new Transaction.Entry(
                new AccountId(row.getAccountId()),
                Transaction.Entry.Direction.valueOf(row.getDirection()),
                new Money(row.getAmount(), currency),
                new Money(row.getBalanceAfter(), currency));
    }

    public static void copyOnto(Transaction tx, TransactionJpaEntity row) {
        row.setId(tx.id().value());
        row.setType(tx.type().name());
        row.setStatus(tx.status().name());
        row.setSenderReference(tx.sender().reference());
        row.setSenderType(tx.sender().type().name());
        row.setReceiverReference(tx.receiver().reference());
        row.setReceiverType(tx.receiver().type().name());
        row.setCreatedAt(tx.createdAt());
        // Entries are append-only; only add the ones the row doesn't have yet.
        for (int i = row.getEntries().size(); i < tx.entries().size(); i++) {
            row.getEntries().add(entryToRow(tx.entries().get(i)));
        }
    }

    private static TransactionJpaEntity.TransactionEntryJpaEntity entryToRow(Transaction.Entry entry) {
        var row = new TransactionJpaEntity.TransactionEntryJpaEntity();
        row.setAccountId(entry.accountId().value());
        row.setDirection(entry.direction().name());
        row.setCurrency(entry.amount().currency().code());
        row.setAmount(entry.amount().amount());
        row.setBalanceAfter(entry.balanceAfter().amount());
        return row;
    }
}
