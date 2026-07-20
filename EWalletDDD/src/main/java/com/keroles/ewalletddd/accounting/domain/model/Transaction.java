package com.keroles.ewalletddd.accounting.domain.model;

import com.keroles.ewalletddd.accounting.domain.valueObject.AccountId;
import com.keroles.ewalletddd.accounting.domain.valueObject.Party;
import com.keroles.ewalletddd.accounting.domain.valueObject.TransactionId;
import com.keroles.ewalletddd.shared.domain.Money;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public class Transaction {

    public enum Type { DEPOSIT, WITHDRAWAL, TOPUP, TRANSFER, CASHOUT }
    public enum Status { PENDING, COMPLETED, FAILED }
    public enum Stage { HOLD, SETTLE, RELEASE }

    public record Entry(AccountId accountId, Direction direction, Money amount, Money balanceAfter) {
        public enum Direction { DEBIT, CREDIT } 
    }

    public record Transfer(AccountId senderId, AccountId receiverId, Money amount) {}

    private final TransactionId id;
    private final Type type;
    private final Party sender;    
    private final Party receiver;  
    private final Money amount;    
    private final Stage stage;
    private final TransactionId parentCorrelationId;
    private final Instant createdAt;
    private Status status;

    private final List<Entry> entries = new ArrayList<>();
    private final List<Transfer> transfers = new ArrayList<>();

    private Transaction(TransactionId id, Type type, Party sender, Party receiver, Money amount, Stage stage,
                         TransactionId parentCorrelationId, Status status, Instant createdAt) {
        this.id = id;
        this.type = type;
        this.sender = sender;
        this.receiver = receiver;
        this.amount = amount;
        this.stage = stage;
        this.parentCorrelationId = parentCorrelationId;
        this.status = status;
        this.createdAt = createdAt;
    }

    public static Transaction start(Type type, Party sender, Party receiver, Money amount) {
        return start(type, sender, receiver, amount, null, null);
    }


    public static Transaction start(Type type, Party sender, Party receiver, Money amount, Stage stage,
                                    TransactionId parentCorrelationId) {
        TransactionId id = TransactionId.newId();
        TransactionId correlationId = parentCorrelationId != null ? parentCorrelationId : id;
        return new Transaction(id, type, sender, receiver, amount, stage, correlationId, Status.PENDING, Instant.now());
    }

    public static Transaction restore(TransactionId id, Type type, Party sender, Party receiver, Money amount,
                                      Stage stage, TransactionId parentCorrelationId, Status status,
                                      Instant createdAt, List<Entry> entries, List<Transfer> transfers) {
        Transaction tx = new Transaction(id, type, sender, receiver, amount, stage, parentCorrelationId, status, createdAt);
        tx.entries.addAll(entries);
        tx.transfers.addAll(transfers);
        return tx;
    }

    public void addEntry(AccountId accountId, Entry.Direction direction, Money amount, Money balanceAfter) {
        if (status != Status.PENDING)
            throw new IllegalStateException("Cannot add entries to a " + status + " transaction");
        entries.add(new Entry(accountId, direction, amount, balanceAfter));
    }

    public void addTransfer(AccountId senderId, AccountId receiverId, Money amount) {
        if (status != Status.COMPLETED)
            throw new IllegalStateException("Cannot add a transfer to a " + status + " transaction");
        transfers.add(new Transfer(senderId, receiverId, amount));
    }

    public void complete() { transitionFromPendingTo(Status.COMPLETED); }
    public void fail()     { transitionFromPendingTo(Status.FAILED); }

    private void transitionFromPendingTo(Status target) {
        if (status != Status.PENDING)
            throw new IllegalStateException("Transaction is already " + status);
        status = target;
    }

    
    public static Transaction deposit(AccountId acc, Party self, Money amount, Money balanceAfter) {
        Transaction tx = start(Type.DEPOSIT, Party.EXTERNAL, self, amount);
        tx.addEntry(acc, Entry.Direction.CREDIT, amount, balanceAfter);
        tx.complete();
        return tx;
    }
    
    public static Transaction withdrawal(AccountId acc, Party self, Money amount, Money balanceAfter) {
        Transaction tx = start(Type.WITHDRAWAL, self, Party.EXTERNAL, amount);
        tx.addEntry(acc, Entry.Direction.DEBIT, amount, balanceAfter);
        tx.complete();
        return tx;
    }

    public TransactionId id() { return id; }
    public Type type() { return type; }
    public Party sender() { return sender; }
    public Party receiver() { return receiver; }
    public Money amount() { return amount; }
    public Stage stage() { return stage; }
    public TransactionId parentCorrelationId() { return parentCorrelationId; }
    public Status status() { return status; }
    public Instant createdAt() { return createdAt; }
    public List<Entry> entries() { return List.copyOf(entries); }
    public List<Transfer> transfers() { return List.copyOf(transfers); }
}
