package com.keroles.ewalletddd.accounting.domain.model;

import com.keroles.ewalletddd.shared.domain.UserId;

import java.time.Instant;

/**
 * Minimal aggregate: Accounting only needs the user to EXIST (referential integrity
 * for accounts). Names, KYC, profile status — that's the Onboarding context's User,
 * not this one. Same word, different context, different model.
 */
public class User {

    private final UserId id;
    private final Instant createdAt;

    private User(UserId id, Instant createdAt) {
        this.id = id;
        this.createdAt = createdAt;
    }

    public static User register() {
        return new User(UserId.newId(), Instant.now());
    }

    public static User restore(UserId id, Instant createdAt) {
        return new User(id, createdAt);
    }

    public UserId id() { return id; }
    public Instant createdAt() { return createdAt; }
}
