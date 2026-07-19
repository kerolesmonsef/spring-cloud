package com.keroles.ewalletddd.cashout.domain.valueObject;

// Cashout's own handle on a Ledger account — the ACL translates it to accounting's AccountId.
public record LedgerAccountRef(Long value) {}
