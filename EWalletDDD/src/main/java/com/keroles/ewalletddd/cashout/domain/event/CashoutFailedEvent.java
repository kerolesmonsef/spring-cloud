package com.keroles.ewalletddd.cashout.domain.event;

import com.keroles.ewalletddd.cashout.domain.valueObject.CashoutId;
import com.keroles.ewalletddd.cashout.domain.valueObject.LedgerReservationRef;

public record CashoutFailedEvent(CashoutId cashoutId, LedgerReservationRef reservationRef) {
}
