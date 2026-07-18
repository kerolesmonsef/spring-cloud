package com.keroles.ewalletddd.accounting.domain.valueObject;

import java.util.UUID;

public record AccountReference(UUID value) {
    public static AccountReference newRef() { return new AccountReference(UUID.randomUUID()); }
}
