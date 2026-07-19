package com.keroles.ewalletddd.accounting.domain.valueObject;

// Account kind. USER = customer wallet (default), SYSTEM = house/settlement account.
// Mirrors the a_account_types lookup rows (lowercased) — same pattern as cashout's Rail enum.
public enum AccountType {
    SYSTEM,
    USER
}
