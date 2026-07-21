package com.keroles.ewalletddd.transfer.infrastructure.ledger;

import com.keroles.ewalletddd.accounting.application.TransactionApplicationService;
import com.keroles.ewalletddd.accounting.domain.valueObject.AccountId;
import com.keroles.ewalletddd.accounting.domain.valueObject.TransactionId;
import com.keroles.ewalletddd.transfer.domain.port.LedgerTransferPort;
import com.keroles.ewalletddd.transfer.domain.valueObject.LedgerAccountRef;
import com.keroles.ewalletddd.transfer.domain.valueObject.LedgerHoldRef;
import com.keroles.ewalletddd.transfer.domain.valueObject.LedgerSettleRef;
import com.keroles.ewalletddd.shared.domain.Money;
import org.springframework.stereotype.Component;

@Component
public class LedgerTransferAdapter implements LedgerTransferPort {

    private final TransactionApplicationService ledger;

    public LedgerTransferAdapter(TransactionApplicationService ledger) {
        this.ledger = ledger;
    }

    @Override
    public LedgerHoldRef hold(LedgerAccountRef fromAccount, LedgerAccountRef toAccount, Money amount) {
        TransactionId tx = ledger.transfer(new AccountId(fromAccount.value()), new AccountId(toAccount.value()), amount);
        return new LedgerHoldRef(tx.value());
    }

    @Override
    public LedgerSettleRef settle(LedgerHoldRef holdRef) {
        TransactionId settle = ledger.settle(new TransactionId(holdRef.value()));
        return new LedgerSettleRef(settle.value());
    }
}
