package com.keroles.ewalletddd.accounting.infrastructure.reference;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

// ponytail: plain lookup table, not a DDD aggregate. Accounting-owned (a_ prefix per repo law). code is the join key back to a_accounts.currency char(3).
@Entity
@Table(name = "a_currencies")
@Getter
@Setter
@NoArgsConstructor
public class CurrencyJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 3)
    private String code; // ISO 4217, e.g. AED

    public CurrencyJpaEntity(String code) {
        this.code = code;
    }
}
