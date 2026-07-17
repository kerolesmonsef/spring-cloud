package com.keroles.ewalletddd.accounting.domain.model;

import com.keroles.ewalletddd.shared.domain.UserId;

import java.time.Instant;

/**
 * Minimal aggregate: Accounting only needs the user to EXIST (referential integrity
 * for accounts). Names, KYC, profile status — that's the Onboarding context's User,
 * not this one. Same word, different context, different model.
 */
public class User {

    private UserId id; // null until first save — DB auto-increment
    private final Instant createdAt;

    private User(UserId id, Instant createdAt) {
        this.id = id;
        this.createdAt = createdAt;
    }

    public static User register() {
        return new User(null, Instant.now());
    }

    public static User restore(UserId id, Instant createdAt) {
        return new User(id, createdAt);
    }

    /** Called ONCE by the persistence adapter after INSERT. */
    public void assignId(UserId id) {
        if (this.id != null) throw new IllegalStateException("User already has id " + this.id.value());
        this.id = id;
    }

    public UserId id() { return id; }
    public Instant createdAt() { return createdAt; }
}
