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

// ponytail: plain lookup table, not a DDD aggregate. Accounting-owned (a_ prefix per repo law). Wire a_accounts FK when actually used.
@Entity
@Table(name = "a_account_types")
@Getter
@Setter
@NoArgsConstructor
public class AccountTypeJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String name;

    public AccountTypeJpaEntity(String name) {
        this.name = name;
    }
}
