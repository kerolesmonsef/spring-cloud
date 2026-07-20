package com.keroles.ewalletddd.cashout.infrastructure.ledger;

import com.keroles.ewalletddd.accounting.application.TransactionApplicationService;
import com.keroles.ewalletddd.accounting.domain.valueObject.AccountId;
import com.keroles.ewalletddd.accounting.domain.valueObject.TransactionId;
import com.keroles.ewalletddd.cashout.domain.port.LedgerAccountPort;
import com.keroles.ewalletddd.cashout.domain.valueObject.LedgerAccountRef;
import com.keroles.ewalletddd.cashout.domain.valueObject.LedgerReservationRef;
import com.keroles.ewalletddd.cashout.domain.valueObject.LedgerSettleRef;
import com.keroles.ewalletddd.shared.domain.Money;
import org.springframework.stereotype.Component;


@Component
public class LedgerAccountAdapter implements LedgerAccountPort {
    private final TransactionApplicationService ledger; 

    public LedgerAccountAdapter(TransactionApplicationService ledger) {
        this.ledger = ledger;
    }

    @Override
    public LedgerReservationRef reserve(LedgerAccountRef account, Money amount) {
        
        TransactionId tx = ledger.cashout(new AccountId(account.value()), amount);
        return new LedgerReservationRef(tx.value());
    }

    @Override
    public LedgerSettleRef settle(LedgerReservationRef reservation) {
        TransactionId settle = ledger.settle(new TransactionId(reservation.value()));
        return new LedgerSettleRef(settle.value());
    }

    @Override
    public void release(LedgerReservationRef reservation) {
        ledger.release(new TransactionId(reservation.value()));
    }
}
