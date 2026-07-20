package com.keroles.ewalletddd.topup.domain.event;

import com.keroles.ewalletddd.topup.domain.valueObject.LedgerAccountRef;
import com.keroles.ewalletddd.topup.domain.valueObject.Rail;
import com.keroles.ewalletddd.topup.domain.valueObject.TopupId;
import com.keroles.ewalletddd.shared.domain.Money;

public record TopupRequestedEvent(TopupId topupId, LedgerAccountRef account, Money amount, Rail rail) {
}
