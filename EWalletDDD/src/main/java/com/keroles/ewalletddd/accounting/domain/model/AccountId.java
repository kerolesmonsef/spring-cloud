package com.keroles.ewalletddd.accounting.domain.model;

/** DB auto-increment assigns the value; null until the aggregate is first saved. */
public record AccountId(Long value) {
}
