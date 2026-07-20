package com.keroles.ewalletddd.topup.domain.valueObject;

// Topup's own handle on a Ledger account — the ACL translates it to accounting's AccountId.
public record LedgerAccountRef(Long value) {}
