package com.keroles.ewalletddd.accounting.domain.event;

import com.keroles.ewalletddd.accounting.domain.valueObject.AccountId;
import com.keroles.ewalletddd.shared.domain.Money;

public record FundsReleasedEvent(AccountId accountId, Money amount, Money balanceAfter, Money holdBalanceAfter) {
}
