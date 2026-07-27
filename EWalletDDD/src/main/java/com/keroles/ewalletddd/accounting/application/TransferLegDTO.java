package com.keroles.ewalletddd.accounting.application;

import com.keroles.ewalletddd.accounting.domain.valueObject.AccountReference;
import com.keroles.ewalletddd.shared.domain.Money;

public record TransferLegDTO(AccountReference senderId, AccountReference receiverId, Money amount) {
}
