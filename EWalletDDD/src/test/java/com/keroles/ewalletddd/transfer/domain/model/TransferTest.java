package com.keroles.ewalletddd.transfer.domain.model;

import com.keroles.ewalletddd.transfer.domain.event.TransferCompletedEvent;
import com.keroles.ewalletddd.transfer.domain.valueObject.LedgerAccountRef;
import com.keroles.ewalletddd.transfer.domain.valueObject.LedgerHoldRef;
import com.keroles.ewalletddd.transfer.domain.valueObject.LedgerSettleRef;
import com.keroles.ewalletddd.shared.domain.Money;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TransferTest {

    private final LedgerAccountRef from = new LedgerAccountRef(1L);
    private final LedgerAccountRef to = new LedgerAccountRef(2L);
    private final LedgerHoldRef holdRef = new LedgerHoldRef(UUID.randomUUID());
    private final LedgerSettleRef settleRef = new LedgerSettleRef(UUID.randomUUID());
    private final Money amount = Money.of("50.00", "AED");

    @Test
    void complete_setsAllFieldsAndRaisesOneEvent() {
        Transfer t = Transfer.complete(from, to, amount, holdRef, settleRef);

        assertEquals(from, t.fromAccount());
        assertEquals(to, t.toAccount());
        assertEquals(amount, t.amount());
        assertEquals(holdRef, t.holdRef());
        assertEquals(settleRef, t.settleRef());

        List<Object> events = t.pullEvents();
        assertEquals(1, events.size());
        assertInstanceOf(TransferCompletedEvent.class, events.get(0));
        assertTrue(t.pullEvents().isEmpty());
    }

    @Test
    void restore_rehydratesWithoutRaisingEvents() {
        Transfer t = Transfer.restore(Transfer.complete(from, to, amount, holdRef, settleRef).id(),
                from, to, amount, holdRef, settleRef, java.time.Instant.now());

        assertTrue(t.pullEvents().isEmpty());
    }
}
