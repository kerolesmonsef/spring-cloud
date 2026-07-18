package com.keroles.ewalletddd.accounting.domain.event;

import com.keroles.ewalletddd.accounting.domain.valueObject.AccountId;
import com.keroles.ewalletddd.shared.domain.Money;

public record MoneyDepositedEvent(AccountId accountId, Money amount, Money balanceAfter) {
}
