package com.keroles.ewalletddd.topup.domain.port;

import com.keroles.ewalletddd.topup.domain.valueObject.Rail;


public interface TopupRailRegistry {
    TopupRailPort forRail(Rail rail);
}
