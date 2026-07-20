package com.keroles.ewalletddd.cashout.domain.port;

import com.keroles.ewalletddd.cashout.domain.valueObject.CashoutId;
import com.keroles.ewalletddd.cashout.domain.valueObject.Rail;
import com.keroles.ewalletddd.shared.domain.Money;


public interface CashoutRailPort {
    Rail rail();
    RailDispatchResult dispatch(CashoutId id, Money amount);
}
