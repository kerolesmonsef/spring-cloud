package com.keroles.ewalletddd.cashout.domain.port;

import com.keroles.ewalletddd.cashout.domain.valueObject.Rail;

// Picks the CashoutRailPort for a rail. Impl builds the map from all adapters; app service depends on this.
public interface CashoutRailRegistry {
    CashoutRailPort forRail(Rail rail);
}
