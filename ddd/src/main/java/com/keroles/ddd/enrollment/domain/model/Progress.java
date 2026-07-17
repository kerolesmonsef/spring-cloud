package com.keroles.ddd.enrollment.domain.model;

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
public class Progress {

    private int percent;

    private Progress(int percent) {
        this.percent = percent;
    }

    public static Progress zero() {
        return new Progress(0);
    }

    public static Progress of(int percent) {
        if (percent < 0 || percent > 100) {
            throw new DomainException("Progress must be between 0 and 100");
        }
        return new Progress(percent);
    }

    public Progress advanceTo(int percent) {
        if (percent < this.percent) {
            throw new DomainException("Progress cannot move backwards");
        }
        return of(percent);
    }

    public boolean isComplete() {
        return percent == 100;
    }
}
