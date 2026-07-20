package com.keroles.ewalletddd.topup.domain.port;

import com.keroles.ewalletddd.topup.domain.valueObject.Rail;
import com.keroles.ewalletddd.topup.domain.valueObject.TopupId;
import com.keroles.ewalletddd.shared.domain.Money;

// Driven port: one implementation per rail. rail() lets the registry self-assemble the routing map.
public interface TopupRailPort {
    Rail rail();
    RailDispatchResult dispatch(TopupId id, Money amount);
}
