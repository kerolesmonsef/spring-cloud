package com.keroles.ewalletddd.cashout.domain.event;

import com.keroles.ewalletddd.cashout.domain.valueObject.CashoutId;
import com.keroles.ewalletddd.cashout.domain.valueObject.Rail;

public record CashoutDispatchedEvent(CashoutId cashoutId, Rail rail, String railReference) {
}
