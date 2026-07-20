package com.keroles.ewalletddd.topup.domain.model;

import com.keroles.ewalletddd.topup.domain.event.TopupCompletedEvent;
import com.keroles.ewalletddd.topup.domain.event.TopupDispatchedEvent;
import com.keroles.ewalletddd.topup.domain.event.TopupFailedEvent;
import com.keroles.ewalletddd.topup.domain.event.TopupRequestedEvent;
import com.keroles.ewalletddd.topup.domain.exception.IllegalTopupStateException;
import com.keroles.ewalletddd.topup.domain.valueObject.LedgerAccountRef;
import com.keroles.ewalletddd.topup.domain.valueObject.LedgerTransactionRef;
import com.keroles.ewalletddd.topup.domain.valueObject.Rail;
import com.keroles.ewalletddd.topup.domain.valueObject.TopupId;
import com.keroles.ewalletddd.shared.domain.Money;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;







public class TopupRequest {

    public enum Status { PENDING, COMPLETED, FAILED }

    private final TopupId id;
    private final LedgerAccountRef account;
    private final Money amount;
    private final Rail rail;
    private final Instant createdAt;
    private Status status;
    private String railReference;                     
    private LedgerTransactionRef ledgerTransactionRef; 
    private final List<Object> events = new ArrayList<>();

    private TopupRequest(TopupId id, LedgerAccountRef account, Money amount, Rail rail, Status status,
                         String railReference, LedgerTransactionRef ledgerTransactionRef, Instant createdAt) {
        this.id = id;
        this.account = account;
        this.amount = amount;
        this.rail = rail;
        this.status = status;
        this.railReference = railReference;
        this.ledgerTransactionRef = ledgerTransactionRef;
        this.createdAt = createdAt;
    }

    public static TopupRequest request(LedgerAccountRef account, Money amount, Rail rail) {
        TopupRequest t = new TopupRequest(TopupId.newId(), account, amount, rail,
                Status.PENDING, null, null, Instant.now());
        t.events.add(new TopupRequestedEvent(t.id, account, amount, rail));
        return t;
    }

    public static TopupRequest restore(TopupId id, LedgerAccountRef account, Money amount, Rail rail, Status status,
                                       String railReference, LedgerTransactionRef ledgerTransactionRef, Instant createdAt) {
        return new TopupRequest(id, account, amount, rail, status, railReference, ledgerTransactionRef, createdAt);
    }

    
    
    public void recordDispatch(String railReference) {
        requireStatus(Status.PENDING);
        this.railReference = railReference;
        events.add(new TopupDispatchedEvent(id, rail, railReference));
    }

    public void complete(LedgerTransactionRef ledgerTransactionRef) {
        requireStatus(Status.PENDING);
        this.ledgerTransactionRef = ledgerTransactionRef;
        status = Status.COMPLETED;
        events.add(new TopupCompletedEvent(id, ledgerTransactionRef));
    }

    public void fail(String reason) {
        requireStatus(Status.PENDING); 
        status = Status.FAILED;
        events.add(new TopupFailedEvent(id, reason));
    }

    private void requireStatus(Status expected) {
        if (status != expected)
            throw new IllegalTopupStateException("Topup " + id.value() + " is " + status + ", expected " + expected);
    }

    public List<Object> pullEvents() {
        List<Object> pulled = List.copyOf(events);
        events.clear();
        return pulled;
    }

    public TopupId id() { return id; }
    public LedgerAccountRef account() { return account; }
    public Money amount() { return amount; }
    public Rail rail() { return rail; }
    public Status status() { return status; }
    public String railReference() { return railReference; }
    public LedgerTransactionRef ledgerTransactionRef() { return ledgerTransactionRef; }
    public Instant createdAt() { return createdAt; }
}
