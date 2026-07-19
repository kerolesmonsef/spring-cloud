package com.keroles.ewalletddd.cashout.domain.port;

import com.keroles.ewalletddd.cashout.domain.valueObject.LedgerAccountRef;
import com.keroles.ewalletddd.cashout.domain.valueObject.LedgerReservationRef;
import com.keroles.ewalletddd.shared.domain.Money;

// Driven port: the Ledger front door in Cashout's terms. Implemented by the ACL adapter.
public interface LedgerAccountPort {
    LedgerReservationRef reserve(LedgerAccountRef account, Money amount);
    void settle(LedgerReservationRef reservation);
    void release(LedgerReservationRef reservation);
}
