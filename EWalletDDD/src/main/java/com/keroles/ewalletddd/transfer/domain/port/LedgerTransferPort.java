package com.keroles.ewalletddd.transfer.domain.port;

import com.keroles.ewalletddd.transfer.domain.valueObject.FeeLeg;
import com.keroles.ewalletddd.transfer.domain.valueObject.LedgerAccountReference;
import com.keroles.ewalletddd.transfer.domain.valueObject.LedgerHoldRef;
import com.keroles.ewalletddd.transfer.domain.valueObject.LedgerSettleRef;
import com.keroles.ewalletddd.shared.domain.Money;

import java.util.List;

public interface LedgerTransferPort {
    LedgerHoldRef hold(LedgerAccountReference fromAccount, LedgerAccountReference toAccount, Money amount);
    LedgerSettleRef settle(LedgerHoldRef holdRef, Money principal, List<FeeLeg> feeLegs);
}
