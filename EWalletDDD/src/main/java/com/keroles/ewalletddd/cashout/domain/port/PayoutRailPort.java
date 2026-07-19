package com.keroles.ewalletddd.cashout.domain.port;

import com.keroles.ewalletddd.cashout.domain.valueObject.CashoutId;
import com.keroles.ewalletddd.cashout.domain.valueObject.Rail;
import com.keroles.ewalletddd.shared.domain.Money;

// Driven port: one implementation per rail. rail() lets the registry self-assemble the routing map.
public interface PayoutRailPort {
    Rail rail();
    RailDispatchResult dispatch(CashoutId id, Money amount);
}
