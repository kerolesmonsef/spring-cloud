package com.keroles.ewalletddd.cashout.domain.model;

import com.keroles.ewalletddd.cashout.domain.event.CashoutConfirmedEvent;
import com.keroles.ewalletddd.cashout.domain.event.CashoutDispatchedEvent;
import com.keroles.ewalletddd.cashout.domain.event.CashoutFailedEvent;
import com.keroles.ewalletddd.cashout.domain.event.CashoutRequestedEvent;
import com.keroles.ewalletddd.cashout.domain.exception.IllegalCashoutStateException;
import com.keroles.ewalletddd.cashout.domain.valueObject.*;
import com.keroles.ewalletddd.shared.domain.Money;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public class CashoutRequest {

    public enum Status { RESERVED, DISPATCHED, CONFIRMED, FAILED }

    private final CashoutId id;
    private final LedgerAccountRef account;
    private final Money amount;
    private final Rail rail;
    private final LedgerReservationRef reservationRef; 
    private final Instant createdAt;
    private Status status;
    private String railReference; 
    private LedgerSettleRef settleReference; 
    private final List<Object> events = new ArrayList<>();

    private CashoutRequest(CashoutId id, LedgerAccountRef account, Money amount, Rail rail,
                           LedgerReservationRef reservationRef, Status status, String railReference, Instant createdAt) {
        this.id = id;
        this.account = account;
        this.amount = amount;
        this.rail = rail;
        this.reservationRef = reservationRef;
        this.status = status;
        this.railReference = railReference;
        this.createdAt = createdAt;
    }

    
    public static CashoutRequest request(LedgerAccountRef account, Money amount, Rail rail, LedgerReservationRef reservationRef) {
        CashoutRequest c = new CashoutRequest(CashoutId.newId(), account, amount, rail,
                reservationRef, Status.RESERVED, null, Instant.now());
        c.events.add(new CashoutRequestedEvent(c.id, account, amount, rail));
        return c;
    }

    public static CashoutRequest restore(CashoutId id, LedgerAccountRef account, Money amount, Rail rail,
                                         LedgerReservationRef reservationRef, Status status, String railReference, Instant createdAt) {
        return new CashoutRequest(id, account, amount, rail, reservationRef, status, railReference, createdAt);
    }

    public void markDispatched(String railReference) {
        requireStatus(Status.RESERVED);
        this.railReference = railReference;
        this.status = Status.DISPATCHED;
        events.add(new CashoutDispatchedEvent(id, rail, railReference));
    }

    public void confirm(LedgerSettleRef settleReference) {
        requireStatus(Status.DISPATCHED); 
        this.settleReference = settleReference; 
        status = Status.CONFIRMED;
        events.add(new CashoutConfirmedEvent(id, reservationRef, settleReference));
    }

    
    public void fail() {
        if (status != Status.RESERVED && status != Status.DISPATCHED)
            throw new IllegalCashoutStateException("Cannot fail cashout " + id.value() + " in state " + status);
        status = Status.FAILED;
        events.add(new CashoutFailedEvent(id, reservationRef));
    }

    private void requireStatus(Status expected) {
        if (status != expected)
            throw new IllegalCashoutStateException("Cashout " + id.value() + " is " + status + ", expected " + expected);
    }

    public List<Object> pullEvents() {
        List<Object> pulled = List.copyOf(events);
        events.clear();
        return pulled;
    }

    public CashoutId id() { return id; }
    public LedgerAccountRef account() { return account; }
    public Money amount() { return amount; }
    public Rail rail() { return rail; }
    public LedgerReservationRef reservationRef() { return reservationRef; }
    public Status status() { return status; }
    public String railReference() { return railReference; }
    public Instant createdAt() { return createdAt; }
    public LedgerSettleRef settleReference() { return settleReference; }
}
