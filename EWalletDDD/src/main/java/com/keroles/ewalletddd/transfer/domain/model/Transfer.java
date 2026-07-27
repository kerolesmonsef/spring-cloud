package com.keroles.ewalletddd.transfer.domain.model;

import com.keroles.ewalletddd.transfer.domain.event.TransferCompletedEvent;
import com.keroles.ewalletddd.transfer.domain.valueObject.LedgerAccountReference;
import com.keroles.ewalletddd.transfer.domain.valueObject.LedgerHoldRef;
import com.keroles.ewalletddd.transfer.domain.valueObject.LedgerSettleRef;
import com.keroles.ewalletddd.transfer.domain.valueObject.TransferId;
import com.keroles.ewalletddd.shared.domain.Money;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public class Transfer {

    private final TransferId id;
    private final LedgerAccountReference fromAccount;
    private final LedgerAccountReference toAccount;
    private final Money amount;
    private final LedgerHoldRef holdRef;
    private final LedgerSettleRef settleRef;
    private final Instant createdAt;
    private final List<Object> events = new ArrayList<>();

    private Transfer(TransferId id, LedgerAccountReference fromAccount, LedgerAccountReference toAccount, Money amount,
                     LedgerHoldRef holdRef, LedgerSettleRef settleRef, Instant createdAt) {
        this.id = id;
        this.fromAccount = fromAccount;
        this.toAccount = toAccount;
        this.amount = amount;
        this.holdRef = holdRef;
        this.settleRef = settleRef;
        this.createdAt = createdAt;
    }

    public static Transfer complete(LedgerAccountReference fromAccount, LedgerAccountReference toAccount, Money amount,
                                    LedgerHoldRef holdRef, LedgerSettleRef settleRef) {
        Transfer t = new Transfer(TransferId.newId(), fromAccount, toAccount, amount, holdRef, settleRef, Instant.now());
        t.events.add(new TransferCompletedEvent(t.id, fromAccount, toAccount, amount));
        return t;
    }

    public static Transfer restore(TransferId id, LedgerAccountReference fromAccount, LedgerAccountReference toAccount, Money amount,
                                   LedgerHoldRef holdRef, LedgerSettleRef settleRef, Instant createdAt) {
        return new Transfer(id, fromAccount, toAccount, amount, holdRef, settleRef, createdAt);
    }

    public List<Object> pullEvents() {
        List<Object> pulled = List.copyOf(events);
        events.clear();
        return pulled;
    }

    public TransferId id() { return id; }
    public LedgerAccountReference fromAccount() { return fromAccount; }
    public LedgerAccountReference toAccount() { return toAccount; }
    public Money amount() { return amount; }
    public LedgerHoldRef holdRef() { return holdRef; }
    public LedgerSettleRef settleRef() { return settleRef; }
    public Instant createdAt() { return createdAt; }
}
