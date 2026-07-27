package com.keroles.ewalletddd.transfer.domain.port;

import com.keroles.ewalletddd.shared.domain.Money;
import com.keroles.ewalletddd.transfer.domain.valueObject.FeeQuote;

public interface PricingPort {
    FeeQuote calculateTransferFees(Money amount);
}
