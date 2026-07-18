package com.keroles.ewalletddd.accounting.domain.model;

import com.keroles.ewalletddd.shared.domain.UserId;

import java.time.Instant;

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

    public void assignId(UserId id) {
        if (this.id != null) throw new IllegalStateException("User already has id " + this.id.value());
        this.id = id;
    }

    public UserId id() { return id; }
    public Instant createdAt() { return createdAt; }
}
