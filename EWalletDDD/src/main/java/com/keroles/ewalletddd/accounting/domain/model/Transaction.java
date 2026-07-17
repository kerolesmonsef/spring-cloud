package com.keroles.ewalletddd.accounting.domain.model;

import com.keroles.ewalletddd.shared.domain.Money;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Aggregate root — the ledger record of one money movement.
 *
 * Separate aggregate from Account: a transaction can span accounts and lives
 * forever (append-only), while an Account is a live balance. They reference
 * each other by ID only (Evans: aggregates hold identities, not objects).
 *
 * Entries are local entities inside this aggregate; each entry snapshots the
 * account balance AFTER the movement (your transaction_entries.updated_balance).
 */
public class Transaction {

    public record TransactionId(UUID value) {
        public static TransactionId newId() { return new TransactionId(UUID.randomUUID()); }
    }

    public enum Type { DEPOSIT, WITHDRAWAL, TRANSFER, CASHOUT }
    public enum Status { PENDING, COMPLETED, FAILED }

    public record Entry(AccountId accountId, Direction direction, Money amount, Money balanceAfter) {
        public enum Direction { DEBIT, CREDIT } // DEBIT = money out of account, CREDIT = money in
    }

    private final TransactionId id;
    private final Type type;
    private final Instant createdAt;
    private Status status;
    private final List<Entry> entries = new ArrayList<>();

    private Transaction(TransactionId id, Type type, Status status, Instant createdAt) {
        this.id = id;
        this.type = type;
        this.status = status;
        this.createdAt = createdAt;
    }

    public static Transaction start(Type type) {
        return new Transaction(TransactionId.newId(), type, Status.PENDING, Instant.now());
    }

    /** Reconstitution from persistence — no business rules run here. */
    public static Transaction restore(TransactionId id, Type type, Status status,
                                      Instant createdAt, List<Entry> entries) {
        Transaction tx = new Transaction(id, type, status, createdAt);
        tx.entries.addAll(entries);
        return tx;
    }

    public void addEntry(AccountId accountId, Entry.Direction direction, Money amount, Money balanceAfter) {
        if (status != Status.PENDING)
            throw new IllegalStateException("Cannot add entries to a " + status + " transaction");
        entries.add(new Entry(accountId, direction, amount, balanceAfter));
    }

    public void complete() { transitionFromPendingTo(Status.COMPLETED); }
    public void fail()     { transitionFromPendingTo(Status.FAILED); }

    private void transitionFromPendingTo(Status target) {
        if (status != Status.PENDING)
            throw new IllegalStateException("Transaction is already " + status);
        status = target;
    }

    public TransactionId id() { return id; }
    public Type type() { return type; }
    public Status status() { return status; }
    public Instant createdAt() { return createdAt; }
    public List<Entry> entries() { return List.copyOf(entries); }
}
