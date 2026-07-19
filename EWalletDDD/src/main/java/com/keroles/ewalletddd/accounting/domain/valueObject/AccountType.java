package com.keroles.ewalletddd.accounting.domain.valueObject;

// Account kind. USER = customer wallet (default), SYSTEM = house/settlement account.
// SYSTEM/USER mirror the a_account_types lookup rows (lowercased) — same pattern as cashout's Rail enum.
// EXTERNAL is NOT a held account and NOT seeded — it only labels a transaction Party we don't own
// (a bank/rail destination or funding source). See Party.
public enum AccountType {
    SYSTEM,
    USER,
    EXTERNAL
}
