package com.keroles.ewalletddd.accounting.infrastructure.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Dumb persistence row. No business logic — the aggregate is Account, this is plumbing.
 * setBalance() exists ONLY here, at the border.
 */
@Entity
@Table(name = "a_accounts")
@Getter
@Setter
@NoArgsConstructor
public class AccountJpaEntity {

    @Id
    @JdbcTypeCode(SqlTypes.CHAR)   // char(36) instead of binary(16) — readable in SQL
    private UUID id;

    /** FK to users. Association exists ONLY so DDL creates the constraint; never navigated. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private UserJpaEntity user;

    /** Read-side mirror of the FK column — mapping reads this, not the lazy proxy. */
    @Column(name = "user_id", nullable = false, insertable = false, updatable = false)
    @JdbcTypeCode(SqlTypes.CHAR)
    private UUID userId;

    @Column(nullable = false, length = 3)
    private String currency;

    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal balance;

    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal holdBalance;

    /** Optimistic lock: two racing updates -> second one throws instead of silently clobbering the ledger. */
    @Version
    private Long version;
}
