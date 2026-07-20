package com.keroles.ewalletddd.cashout.domain.port;

import com.keroles.ewalletddd.cashout.domain.valueObject.Rail;


public interface CashoutRailRegistry {
    CashoutRailPort forRail(Rail rail);
}
