package com.keroles.ewalletddd.topup.domain.valueObject;

import java.util.UUID;

public record TopupId(UUID value) {
    public static TopupId newId() { return new TopupId(UUID.randomUUID()); }
}
