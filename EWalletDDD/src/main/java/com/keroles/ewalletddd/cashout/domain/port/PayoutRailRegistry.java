package com.keroles.ewalletddd.cashout.domain.port;

import com.keroles.ewalletddd.cashout.domain.valueObject.Rail;

// Picks the PayoutRailPort for a rail. Impl builds the map from all adapters; app service depends on this.
public interface PayoutRailRegistry {
    PayoutRailPort forRail(Rail rail);
}
