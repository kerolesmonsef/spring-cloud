package com.keroles.ewalletddd.accounting.infrastructure.persistence.mapper;
  import com.keroles.ewalletddd.accounting.infrastructure.persistence.entity.TransactionJpaEntity;
import com.keroles.ewalletddd.accounting.infrastructure.persistence.entity.TransactionEntryJpaEntity;
import com.keroles.ewalletddd.accounting.infrastructure.persistence.entity.TransferJpaEntity;

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
                new Money(row.getAmount(), Currency.of(row.getCurrency())),
                row.getStage() == null ? null : Transaction.Stage.valueOf(row.getStage()),
                row.getParentCorrelationId() == null ? null : new TransactionId(row.getParentCorrelationId()),
                Transaction.Status.valueOf(row.getStatus()),
                row.getCreatedAt(),
                row.getEntries().stream().map(TransactionMapper::entryToDomain).toList(),
                row.getTransfers().stream().map(TransactionMapper::transferToDomain).toList());
    }

    private static Transaction.Entry entryToDomain(TransactionEntryJpaEntity row) {
        Currency currency = Currency.of(row.getCurrency());
        return new Transaction.Entry(
                new AccountId(row.getAccountId()),
                Transaction.Entry.Direction.valueOf(row.getDirection()),
                new Money(row.getAmount(), currency),
                new Money(row.getBalanceAfter(), currency));
    }

    private static Transaction.Transfer transferToDomain(TransferJpaEntity row) {
        Currency currency = Currency.of(row.getCurrency());
        return new Transaction.Transfer(
                new AccountId(row.getSenderId()),
                new AccountId(row.getReceiverId()),
                new Money(row.getAmount(), currency));
    }

    public static void copyOnto(Transaction tx, TransactionJpaEntity row) {
        row.setId(tx.id().value());
        row.setType(tx.type().name());
        row.setStatus(tx.status().name());
        row.setSenderReference(tx.sender().reference());
        row.setSenderType(tx.sender().type().name());
        row.setReceiverReference(tx.receiver().reference());
        row.setReceiverType(tx.receiver().type().name());
        row.setAmount(tx.amount().amount());
        row.setCurrency(tx.amount().currency().code());
        row.setStage(tx.stage() == null ? null : tx.stage().name());
        row.setParentCorrelationId(tx.parentCorrelationId() == null ? null : tx.parentCorrelationId().value());
        row.setCreatedAt(tx.createdAt());
        // Entries are append-only; only add the ones the row doesn't have yet.
        for (int i = row.getEntries().size(); i < tx.entries().size(); i++) {
            row.getEntries().add(entryToRow(tx.entries().get(i)));
        }
        // Transfers are append-only; only add the ones the row doesn't have yet.
        for (int i = row.getTransfers().size(); i < tx.transfers().size(); i++) {
            row.getTransfers().add(transferToRow(tx.transfers().get(i)));
        }
    }

    private static TransactionEntryJpaEntity entryToRow(Transaction.Entry entry) {
        var row = new TransactionEntryJpaEntity();
        row.setAccountId(entry.accountId().value());
        row.setDirection(entry.direction().name());
        row.setCurrency(entry.amount().currency().code());
        row.setAmount(entry.amount().amount());
        row.setBalanceAfter(entry.balanceAfter().amount());
        return row;
    }

    private static TransferJpaEntity transferToRow(Transaction.Transfer transfer) {
        var row = new TransferJpaEntity();
        row.setSenderId(transfer.senderId().value());
        row.setReceiverId(transfer.receiverId().value());
        row.setCurrency(transfer.amount().currency().code());
        row.setAmount(transfer.amount().amount());
        return row;
    }
}
