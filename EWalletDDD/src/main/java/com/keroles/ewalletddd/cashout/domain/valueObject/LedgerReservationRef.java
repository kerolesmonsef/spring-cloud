package com.keroles.ewalletddd.cashout.domain.valueObject;

import java.util.UUID;

// Opaque handle on the Ledger hold created by reserve() — the ACL translates it to accounting's TransactionId.
public record LedgerReservationRef(UUID value) {}
