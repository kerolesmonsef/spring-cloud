package com.keroles.ewalletddd.topup.domain.valueObject;

import java.util.UUID;

// Audit link to the Ledger transaction produced by a successful topup — the ACL translates it
// from accounting's TransactionId. Null until the topup completes; write-only within Topup.
public record LedgerTransactionRef(UUID value) {}
