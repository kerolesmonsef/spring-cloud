package com.keroles.ddd.sharedkernel;

import jakarta.persistence.Embeddable;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;

import static lombok.AccessLevel.PROTECTED;

@Embeddable
@Getter
@EqualsAndHashCode
@NoArgsConstructor(access = PROTECTED)
public class PersonName {

    private String value;

    private PersonName(String value) {
        this.value = value;
    }

    public static PersonName of(String value) {
        if (value == null || value.isBlank()) {
            throw new DomainException("Person name cannot be blank");
        }
        String trimmed = value.trim();
        if (trimmed.length() < 2) {
            throw new DomainException("Person name must be at least 2 characters");
        }
        return new PersonName(trimmed);
    }

    @Override
    public String toString() {
        return value;
    }
}
