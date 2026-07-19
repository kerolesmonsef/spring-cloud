package com.keroles.ewalletddd.cashout.domain.event;

import com.keroles.ewalletddd.cashout.domain.valueObject.CashoutId;
import com.keroles.ewalletddd.cashout.domain.valueObject.LedgerAccountRef;
import com.keroles.ewalletddd.cashout.domain.valueObject.Rail;
import com.keroles.ewalletddd.shared.domain.Money;

public record CashoutRequestedEvent(CashoutId cashoutId, LedgerAccountRef account, Money amount, Rail rail) {
}
