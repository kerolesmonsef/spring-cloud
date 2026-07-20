package com.keroles.ewalletddd.topup.domain.port;

import com.keroles.ewalletddd.topup.domain.valueObject.Rail;

// Picks the TopupRailPort for a rail. Impl builds the map from all adapters; app service depends on this.
public interface TopupRailRegistry {
    TopupRailPort forRail(Rail rail);
}
