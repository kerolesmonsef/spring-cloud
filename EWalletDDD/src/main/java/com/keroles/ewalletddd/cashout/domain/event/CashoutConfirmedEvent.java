package com.keroles.ewalletddd.cashout.domain.event;

import com.keroles.ewalletddd.cashout.domain.valueObject.CashoutId;
import com.keroles.ewalletddd.cashout.domain.valueObject.LedgerReservationRef;
import com.keroles.ewalletddd.cashout.domain.valueObject.LedgerSettleRef;

public record CashoutConfirmedEvent(CashoutId cashoutId, LedgerReservationRef reservationRef, LedgerSettleRef settleReference) {
}
