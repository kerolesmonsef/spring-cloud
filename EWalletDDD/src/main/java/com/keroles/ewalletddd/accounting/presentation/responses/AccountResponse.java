package com.keroles.ewalletddd.accounting.presentation.responses;

import com.keroles.ewalletddd.accounting.domain.model.Account;

import java.math.BigDecimal;

public record AccountResponse(Long id, Long userId, String currency,
                              BigDecimal balance, BigDecimal holdBalance) {
    public static AccountResponse from(Account account) {
        return new AccountResponse(
                account.id().value(),
                account.userId().value(),
                account.currency().getCurrencyCode(),
                account.balance().amount(),
                account.holdBalance().amount());
    }
}
