package com.keroles.ewalletddd.topup.domain.event;

import com.keroles.ewalletddd.topup.domain.valueObject.TopupId;

// carries only the id: the Ledger ref is attached AFTER complete() (guard-before-ledger), so it
// isn't available when this event is raised inside the aggregate.
public record TopupCompletedEvent(TopupId topupId) {
}
