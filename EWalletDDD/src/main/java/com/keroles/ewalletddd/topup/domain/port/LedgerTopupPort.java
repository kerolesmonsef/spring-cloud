package com.keroles.ewalletddd.topup.domain.port;

import com.keroles.ewalletddd.topup.domain.valueObject.LedgerAccountRef;
import com.keroles.ewalletddd.topup.domain.valueObject.LedgerTransactionRef;
import com.keroles.ewalletddd.topup.domain.valueObject.TopupId;
import com.keroles.ewalletddd.shared.domain.Money;





public interface LedgerTopupPort {
    LedgerTransactionRef topup(LedgerAccountRef account, Money amount);
}
