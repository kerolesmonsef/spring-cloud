package com.keroles.ewalletddd.accounting.domain.valueObject;

import java.util.UUID;

public record TransactionId(UUID value) {
    public static TransactionId newId() { return new TransactionId(UUID.randomUUID()); }
}
