package com.keroles.ewalletddd.topup.infrastructure.ledger;

import com.keroles.ewalletddd.accounting.application.TransactionApplicationService;
import com.keroles.ewalletddd.accounting.domain.valueObject.AccountId;
import com.keroles.ewalletddd.accounting.domain.valueObject.TransactionId;
import com.keroles.ewalletddd.topup.domain.port.LedgerTopupPort;
import com.keroles.ewalletddd.topup.domain.valueObject.LedgerAccountRef;
import com.keroles.ewalletddd.topup.domain.valueObject.LedgerTransactionRef;
import com.keroles.ewalletddd.topup.domain.valueObject.TopupId;
import com.keroles.ewalletddd.shared.domain.Money;
import org.springframework.stereotype.Component;


@Component
public class LedgerTopupAdapter implements LedgerTopupPort {

    private final TransactionApplicationService ledger; 

    public LedgerTopupAdapter(TransactionApplicationService ledger) {
        this.ledger = ledger;
    }

    @Override
    public LedgerTransactionRef topup(LedgerAccountRef account, Money amount) {
        TransactionId tx = ledger.topup(new AccountId(account.value()), amount);
        return new LedgerTransactionRef(tx.value());
    }
}
