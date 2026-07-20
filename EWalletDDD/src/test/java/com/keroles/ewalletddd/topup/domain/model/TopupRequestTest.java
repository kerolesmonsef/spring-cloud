package com.keroles.ewalletddd.topup.domain.model;

import com.keroles.ewalletddd.topup.domain.exception.IllegalTopupStateException;
import com.keroles.ewalletddd.topup.domain.valueObject.LedgerAccountRef;
import com.keroles.ewalletddd.topup.domain.valueObject.LedgerTransactionRef;
import com.keroles.ewalletddd.topup.domain.valueObject.Rail;
import com.keroles.ewalletddd.shared.domain.Money;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class TopupRequestTest {

    private final LedgerAccountRef account = new LedgerAccountRef(1L);
    private final Money amount = Money.of("50.00", "AED");
    private final LedgerTransactionRef ledgerRef = new LedgerTransactionRef(UUID.randomUUID());

    private TopupRequest pending() {
        return TopupRequest.request(account, amount, Rail.TCS);
    }

    @Test
    void happyPath_pendingToCompleted() {
        TopupRequest t = pending();
        assertEquals(TopupRequest.Status.PENDING, t.status());
        assertNull(t.railReference());

        t.recordDispatch("TCS-1");
        assertEquals(TopupRequest.Status.PENDING, t.status()); 
        assertEquals("TCS-1", t.railReference());

        t.complete(ledgerRef);
        assertEquals(TopupRequest.Status.COMPLETED, t.status());
        assertEquals(ledgerRef, t.ledgerTransactionRef());
    }

    @Test
    void failFromPending() {
        TopupRequest t = pending();
        t.fail("rail reported failure");
        assertEquals(TopupRequest.Status.FAILED, t.status());
    }

    @Test
    void doubleCompleteIsRejected() {
        TopupRequest t = pending();
        t.complete(ledgerRef);
        assertThrows(IllegalTopupStateException.class, () -> t.complete(ledgerRef));
    }

    @Test
    void completeAfterFailIsRejected() {
        TopupRequest t = pending();
        t.fail("nope");
        assertThrows(IllegalTopupStateException.class, () -> t.complete(ledgerRef));
    }

    @Test
    void failAfterCompleteIsRejected() {
        TopupRequest t = pending();
        t.complete(ledgerRef);
        assertThrows(IllegalTopupStateException.class, () -> t.fail("too late"));
    }

    @Test
    void cannotDispatchAfterCompleted() {
        TopupRequest t = pending();
        t.complete(ledgerRef);
        assertThrows(IllegalTopupStateException.class, () -> t.recordDispatch("TCS-2"));
    }
}
