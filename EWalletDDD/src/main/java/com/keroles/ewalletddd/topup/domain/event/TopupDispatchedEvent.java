package com.keroles.ewalletddd.topup.domain.event;

import com.keroles.ewalletddd.topup.domain.valueObject.Rail;
import com.keroles.ewalletddd.topup.domain.valueObject.TopupId;

public record TopupDispatchedEvent(TopupId topupId, Rail rail, String railReference) {
}
