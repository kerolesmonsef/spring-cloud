package com.keroles.ewalletddd.cashout.domain.port;

import com.keroles.ewalletddd.cashout.domain.valueObject.LedgerAccountRef;
import com.keroles.ewalletddd.cashout.domain.valueObject.LedgerReservationRef;
import com.keroles.ewalletddd.cashout.domain.valueObject.LedgerSettleRef;
import com.keroles.ewalletddd.shared.domain.Money;


public interface LedgerAccountPort {
    LedgerReservationRef reserve(LedgerAccountRef account, Money amount);
    LedgerSettleRef settle(LedgerReservationRef reservation);
    void release(LedgerReservationRef reservation);
}
