package com.keroles.ewalletddd.accounting.domain.event;

import com.keroles.ewalletddd.accounting.domain.model.AccountId;
import com.keroles.ewalletddd.shared.domain.UserId;

import java.util.Currency;

public record AccountOpenedEvent(AccountId accountId, UserId userId, Currency currency) {
}
