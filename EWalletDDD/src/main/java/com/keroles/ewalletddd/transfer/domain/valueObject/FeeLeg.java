package com.keroles.ewalletddd.transfer.domain.valueObject;

import com.keroles.ewalletddd.shared.domain.Money;

public record FeeLeg(LedgerAccountReference senderId, LedgerAccountReference receiverId, Money amount) {
}
