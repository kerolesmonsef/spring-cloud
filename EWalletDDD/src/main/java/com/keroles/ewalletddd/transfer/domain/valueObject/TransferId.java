package com.keroles.ewalletddd.transfer.domain.valueObject;

import java.util.UUID;

public record TransferId(UUID value) {
    public static TransferId newId() { return new TransferId(UUID.randomUUID()); }
}
