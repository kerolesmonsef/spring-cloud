package com.keroles.ewalletddd.transfer.domain.port;

import com.keroles.ewalletddd.transfer.domain.valueObject.LedgerAccountRef;
import com.keroles.ewalletddd.transfer.domain.valueObject.LedgerHoldRef;
import com.keroles.ewalletddd.transfer.domain.valueObject.LedgerSettleRef;
import com.keroles.ewalletddd.shared.domain.Money;

public interface LedgerTransferPort {
    LedgerHoldRef hold(LedgerAccountRef fromAccount, LedgerAccountRef toAccount, Money amount);
    LedgerSettleRef settle(LedgerHoldRef holdRef);
}
