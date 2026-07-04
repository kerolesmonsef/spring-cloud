package com.keroles.ddd.enrollment.domain;

import com.keroles.ddd.sharedkernel.DomainException;
import jakarta.persistence.Embeddable;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;

import static lombok.AccessLevel.PROTECTED;

@Embeddable
@Getter
@EqualsAndHashCode
@NoArgsConstructor(access = PROTECTED)
public class Rating {

    private int value;

    private Rating(int value) {
        this.value = value;
    }

    public static Rating of(int value) {
        if (value < 1 || value > 5) {
            throw new DomainException("Rating must be between 1 and 5");
        }
        return new Rating(value);
    }
}
