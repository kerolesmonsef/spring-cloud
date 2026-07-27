package com.keroles.ewalletddd.accounting.domain.valueObject;

import java.util.UUID;

public record AccountReference(String value) {
    public static final AccountReference FEE = new AccountReference("FEE");
    public static final AccountReference VAT = new AccountReference("VAT");

    public static AccountReference newRef() { return new AccountReference(UUID.randomUUID().toString()); }
}
