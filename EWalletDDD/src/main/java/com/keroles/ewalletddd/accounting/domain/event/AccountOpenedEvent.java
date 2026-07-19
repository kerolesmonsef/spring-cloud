package com.keroles.ewalletddd.accounting.domain.event;

import com.keroles.ewalletddd.accounting.domain.valueObject.AccountId;
import com.keroles.ewalletddd.shared.domain.Currency;
import com.keroles.ewalletddd.shared.domain.UserId;

public record AccountOpenedEvent(AccountId accountId, UserId userId, Currency currency) {
}
