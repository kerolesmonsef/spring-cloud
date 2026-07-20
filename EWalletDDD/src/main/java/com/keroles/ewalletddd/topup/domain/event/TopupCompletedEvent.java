package com.keroles.ewalletddd.topup.domain.event;

import com.keroles.ewalletddd.topup.domain.valueObject.LedgerTransactionRef;
import com.keroles.ewalletddd.topup.domain.valueObject.TopupId;

public record TopupCompletedEvent(TopupId topupId, LedgerTransactionRef ledgerTransactionRef) {
}
