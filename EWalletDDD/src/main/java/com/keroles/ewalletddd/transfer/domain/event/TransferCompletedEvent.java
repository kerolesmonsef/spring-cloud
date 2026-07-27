package com.keroles.ewalletddd.transfer.domain.event;

import com.keroles.ewalletddd.transfer.domain.valueObject.LedgerAccountReference;
import com.keroles.ewalletddd.transfer.domain.valueObject.TransferId;
import com.keroles.ewalletddd.shared.domain.Money;

public record TransferCompletedEvent(TransferId transferId, LedgerAccountReference fromAccount, LedgerAccountReference toAccount, Money amount) {
}
