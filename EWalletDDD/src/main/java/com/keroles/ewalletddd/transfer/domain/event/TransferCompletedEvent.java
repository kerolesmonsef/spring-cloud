package com.keroles.ewalletddd.transfer.domain.event;

import com.keroles.ewalletddd.transfer.domain.valueObject.LedgerAccountRef;
import com.keroles.ewalletddd.transfer.domain.valueObject.TransferId;
import com.keroles.ewalletddd.shared.domain.Money;

public record TransferCompletedEvent(TransferId transferId, LedgerAccountRef fromAccount, LedgerAccountRef toAccount, Money amount) {
}
