package com.keroles.ewalletddd.accounting.infrastructure.persistence.entity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderColumn;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "a_transactions")
@Getter
@Setter
@NoArgsConstructor
public class TransactionJpaEntity {

    @Id
    @JdbcTypeCode(SqlTypes.CHAR)   // char(36) instead of binary(16) — readable in SQL
    private UUID id;

    @Column(nullable = false, length = 20)
    private String type;

    @Column(nullable = false, length = 20)
    private String status;

    @Column(nullable = false)
    private Instant createdAt;

    @Version
    private Long version;

    /** Entries live and die with their transaction — one aggregate, one cascade. */
    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name = "transaction_id", nullable = false)
    @OrderColumn(name = "position")
    private List<TransactionEntryJpaEntity> entries = new ArrayList<>();

    @Entity
    @Table(name = "a_transaction_entries")
    @Getter
    @Setter
    @NoArgsConstructor
    public static class TransactionEntryJpaEntity {

        @Id
        @GeneratedValue(strategy = GenerationType.IDENTITY)
        private Long id;

        @Column(nullable = false)
        @JdbcTypeCode(SqlTypes.CHAR)
        private UUID accountId;

        @Column(nullable = false, length = 10)
        private String direction;

        @Column(nullable = false, length = 3)
        private String currency;

        @Column(nullable = false, precision = 19, scale = 4)
        private BigDecimal amount;

        @Column(nullable = false, precision = 19, scale = 4)
        private BigDecimal balanceAfter;
    }
}
