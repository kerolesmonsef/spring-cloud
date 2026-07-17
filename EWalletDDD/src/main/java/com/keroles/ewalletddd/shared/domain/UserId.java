package com.keroles.ewalletddd.shared.domain;

/** Shared identity VO — DB auto-increment assigns the value; domain never invents it. */
public record UserId(Long value) {
}
