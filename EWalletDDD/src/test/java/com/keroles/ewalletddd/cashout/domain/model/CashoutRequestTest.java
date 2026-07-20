package com.keroles.ewalletddd.cashout.domain.model;

import com.keroles.ewalletddd.cashout.domain.exception.IllegalCashoutStateException;
import com.keroles.ewalletddd.cashout.domain.valueObject.LedgerAccountRef;
import com.keroles.ewalletddd.cashout.domain.valueObject.LedgerReservationRef;
import com.keroles.ewalletddd.cashout.domain.valueObject.LedgerSettleRef;
import com.keroles.ewalletddd.cashout.domain.valueObject.Rail;
import com.keroles.ewalletddd.shared.domain.Money;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class CashoutRequestTest {

    private final LedgerAccountRef account = new LedgerAccountRef(1L);
    private final LedgerReservationRef reservation = new LedgerReservationRef(UUID.randomUUID());
    private final LedgerSettleRef settleRef = new LedgerSettleRef(UUID.randomUUID());
    private final Money amount = Money.of("50.00", "AED");

    private CashoutRequest reserved() {
        return CashoutRequest.request(account, amount, Rail.AANI, reservation);
    }

    @Test
    void happyPath_reservedToDispatchedToConfirmed() {
        CashoutRequest c = reserved();
        assertEquals(CashoutRequest.Status.RESERVED, c.status());

        c.markDispatched("AANI-1");
        assertEquals(CashoutRequest.Status.DISPATCHED, c.status());
        assertEquals("AANI-1", c.railReference());

        c.confirm(settleRef);
        assertEquals(CashoutRequest.Status.CONFIRMED, c.status());
        assertEquals(settleRef, c.settleReference());
    }

    @Test
    void dispatchRejected_failFromReserved() {
        CashoutRequest c = reserved();
        c.fail(); 
        assertEquals(CashoutRequest.Status.FAILED, c.status());
    }

    @Test
    void doubleConfirmIsRejected() {
        CashoutRequest c = reserved();
        c.markDispatched("AANI-1");
        c.confirm(settleRef);
        assertThrows(IllegalCashoutStateException.class, () -> c.confirm(settleRef));
    }

    @Test
    void confirmBeforeDispatchIsRejected() {
        assertThrows(IllegalCashoutStateException.class, () -> reserved().confirm(settleRef));
    }

    @Test
    void cannotDispatchTwice() {
        CashoutRequest c = reserved();
        c.markDispatched("AANI-1");
        assertThrows(IllegalCashoutStateException.class, () -> c.markDispatched("AANI-2"));
    }
}
