package com.keroles.ewalletddd.cashout.domain.valueObject;

import java.util.UUID;

public record CashoutId(UUID value) {
    public static CashoutId newId() { return new CashoutId(UUID.randomUUID()); }
}
