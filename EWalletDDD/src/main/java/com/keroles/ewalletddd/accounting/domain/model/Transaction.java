package com.keroles.ewalletddd.accounting.domain.model;

import com.keroles.ewalletddd.accounting.domain.valueObject.AccountId;
import com.keroles.ewalletddd.accounting.domain.valueObject.Party;
import com.keroles.ewalletddd.accounting.domain.valueObject.TransactionId;
import com.keroles.ewalletddd.shared.domain.Money;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public class Transaction {

    public enum Type { DEPOSIT, WITHDRAWAL, TRANSFER, CASHOUT }
    public enum Status { PENDING, COMPLETED, FAILED }

    public record Entry(AccountId accountId, Direction direction, Money amount, Money balanceAfter) {
        public enum Direction { DEBIT, CREDIT } // DEBIT = money out of account, CREDIT = money in
    }

    private final TransactionId id;
    private final Type type;
    private final Party sender;    // business party money moved FROM (may be EXTERNAL — not a held account)
    private final Party receiver;  // business party money moved TO   (may be EXTERNAL)
    private final Instant createdAt;
    private Status status;
    // Ledger postings against accounts WE hold. The header (sender/receiver) names the business parties
    // incl. external ones; entries carry balanceAfter, which only exists for accounts we keep.
    private final List<Entry> entries = new ArrayList<>();

    private Transaction(TransactionId id, Type type, Party sender, Party receiver, Status status, Instant createdAt) {
        this.id = id;
        this.type = type;
        this.sender = sender;
        this.receiver = receiver;
        this.status = status;
        this.createdAt = createdAt;
    }

    public static Transaction start(Type type, Party sender, Party receiver) {
        return new Transaction(TransactionId.newId(), type, sender, receiver, Status.PENDING, Instant.now());
    }

    public static Transaction restore(TransactionId id, Type type, Party sender, Party receiver, Status status,
                                      Instant createdAt, List<Entry> entries) {
        Transaction tx = new Transaction(id, type, sender, receiver, status, createdAt);
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

    // money in from outside: EXTERNAL funding source -> our account
    public static Transaction deposit(AccountId acc, Party self, Money amount, Money balanceAfter) {
        Transaction tx = start(Type.DEPOSIT, Party.EXTERNAL, self);
        tx.addEntry(acc, Entry.Direction.CREDIT, amount, balanceAfter);
        tx.complete();
        return tx;
    }
    // money out to outside: our account -> EXTERNAL destination
    public static Transaction withdrawal(AccountId acc, Party self, Money amount, Money balanceAfter) {
        Transaction tx = start(Type.WITHDRAWAL, self, Party.EXTERNAL);
        tx.addEntry(acc, Entry.Direction.DEBIT, amount, balanceAfter);
        tx.complete();
        return tx;
    }

    public TransactionId id() { return id; }
    public Type type() { return type; }
    public Party sender() { return sender; }
    public Party receiver() { return receiver; }
    public Status status() { return status; }
    public Instant createdAt() { return createdAt; }
    public List<Entry> entries() { return List.copyOf(entries); }
}
