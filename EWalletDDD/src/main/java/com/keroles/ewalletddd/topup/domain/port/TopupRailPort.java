package com.keroles.ewalletddd.topup.domain.port;

import com.keroles.ewalletddd.topup.domain.valueObject.Rail;
import com.keroles.ewalletddd.topup.domain.valueObject.TopupId;
import com.keroles.ewalletddd.shared.domain.Money;


public interface TopupRailPort {
    Rail rail();
    RailDispatchResult dispatch(TopupId id, Money amount);
}
