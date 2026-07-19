package com.keroles.ewalletddd.cashout.infrastructure.ledger;

import com.keroles.ewalletddd.accounting.application.TransactionApplicationService;
import com.keroles.ewalletddd.accounting.domain.valueObject.AccountId;
import com.keroles.ewalletddd.accounting.domain.valueObject.TransactionId;
import com.keroles.ewalletddd.cashout.domain.port.LedgerAccountPort;
import com.keroles.ewalletddd.cashout.domain.valueObject.LedgerAccountRef;
import com.keroles.ewalletddd.cashout.domain.valueObject.LedgerReservationRef;
import com.keroles.ewalletddd.shared.domain.Money;
import org.springframework.stereotype.Component;

/**
 * The Anti-Corruption Layer between Cashout and Accounting.
 * The ONLY class in the cashout context allowed to import accounting.* — it translates
 * Cashout's refs to/from the Ledger front door's identifiers. Everything else stays insulated.
 */
@Component
public class LedgerAccountAdapter implements LedgerAccountPort {

    private final TransactionApplicationService ledger; // accounting.application — the movement front door

    public LedgerAccountAdapter(TransactionApplicationService ledger) {
        this.ledger = ledger;
    }

    @Override
    public LedgerReservationRef reserve(LedgerAccountRef account, Money amount) {
        // cashout holds the user's funds against the house account; the reservation IS the ledger tx
        TransactionId tx = ledger.cashout(new AccountId(account.value()), amount);
        return new LedgerReservationRef(tx.value());
    }

    @Override
    public void settle(LedgerReservationRef reservation) {
        ledger.settle(new TransactionId(reservation.value()));
    }

    @Override
    public void release(LedgerReservationRef reservation) {
        ledger.release(new TransactionId(reservation.value()));
    }
}
