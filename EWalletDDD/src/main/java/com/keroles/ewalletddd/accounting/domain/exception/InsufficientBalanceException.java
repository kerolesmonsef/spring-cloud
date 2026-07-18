package com.keroles.ewalletddd.accounting.domain.exception;

import com.keroles.ewalletddd.accounting.domain.valueObject.AccountId;

import com.keroles.ewalletddd.shared.domain.Money;

public class InsufficientBalanceException extends RuntimeException {
    public InsufficientBalanceException(AccountId accountId, Money balance, Money requested) {
        super("Account " + (accountId == null ? "(unsaved)" : accountId.value())
                + " has " + balance + ", requested " + requested);
    }
}
