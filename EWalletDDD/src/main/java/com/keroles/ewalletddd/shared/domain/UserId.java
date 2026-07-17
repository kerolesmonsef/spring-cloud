package com.keroles.ewalletddd.shared.domain;

import java.util.UUID;

/** Shared identity VO — the only thing Accounting knows about a user. */
public record UserId(UUID value) {
    public static UserId newId() {
        return new UserId(UUID.randomUUID());
    }
}
