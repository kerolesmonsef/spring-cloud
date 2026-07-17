package com.keroles.ewalletddd.accounting.domain.exception;

import com.keroles.ewalletddd.accounting.domain.model.AccountId;

import com.keroles.ewalletddd.shared.domain.Money;

/** Domain exception — named in the ubiquitous language, not a generic IllegalState. */
public class InsufficientBalanceException extends RuntimeException {
    public InsufficientBalanceException(AccountId accountId, Money balance, Money requested) {
        super("Account " + (accountId == null ? "(unsaved)" : accountId.value())
                + " has " + balance + ", requested " + requested);
    }
}
