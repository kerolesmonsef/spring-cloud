package com.keroles.ewalletddd.cashout.infrastructure.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "c_cashout_requests")
@Getter
@Setter
@NoArgsConstructor
public class CashoutRequestJpaEntity {

    @Id
    @JdbcTypeCode(SqlTypes.CHAR)   // char(36) instead of binary(16) — readable in SQL
    private UUID id;

    @Column(nullable = false)
    private Long accountRef;

    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal amount;

    @Column(nullable = false, length = 3)
    private String currency;

    @Column(nullable = false, length = 10)
    private String rail;

    @Column(nullable = false, length = 20)
    private String status;

    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(nullable = false, length = 36)
    private UUID ledgerReservationRef;

    @Column(length = 100)
    private String railReference; // null until dispatched

    @Column(nullable = false)
    private Instant createdAt;

    @Version
    private Long version;
}
