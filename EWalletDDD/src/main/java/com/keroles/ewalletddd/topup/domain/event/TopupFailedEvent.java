package com.keroles.ewalletddd.topup.domain.event;

import com.keroles.ewalletddd.topup.domain.valueObject.TopupId;

public record TopupFailedEvent(TopupId topupId, String reason) {
}
