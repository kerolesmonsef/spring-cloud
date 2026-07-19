package com.keroles.ewalletddd.accounting.domain.valueObject;

// A transaction endpoint (sender or receiver). Value Object: no identity, defined by its attributes.
// Same pattern as cashout's LedgerAccountRef — a reference pointing at something outside the aggregate.
// reference is a String (not AccountReference) so external endpoints can carry an IBAN/rail address;
// internal endpoints carry their account's AccountReference UUID stringified.
public record Party(String reference, AccountType type) {

    // ponytail: fixed sentinel for a counterparty we don't hold; the cashout work threads the real
    // external destination (IBAN/rail addr) into reserve() instead of this placeholder.
    public static final Party EXTERNAL = new Party("EXTERNAL", AccountType.EXTERNAL);

    public static Party internal(AccountReference reference, AccountType type) {
        return new Party(reference.value().toString(), type);
    }
}
