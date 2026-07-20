package com.keroles.ewalletddd.accounting.infrastructure.persistence.entity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
    @JdbcTypeCode(SqlTypes.CHAR)   
    private UUID id;

    @Column(nullable = false, length = 20)
    private String type;

    @Column(nullable = false, length = 20)
    private String status;

    @Column(nullable = false, length = 64)
    private String senderReference;

    @Column(nullable = false, length = 20)
    private String senderType;

    @Column(nullable = false, length = 64)
    private String receiverReference;

    @Column(nullable = false, length = 20)
    private String receiverType;

    
    @Column(precision = 19, scale = 4)
    private BigDecimal amount;

    @Column(length = 3)
    private String currency;

    @Column(length = 10)
    private String stage;

    @JdbcTypeCode(SqlTypes.CHAR)
    private UUID parentCorrelationId;

    @Column(nullable = false)
    private Instant createdAt;

    @Version
    private Long version;

    
    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name = "transaction_id", nullable = false)
    @OrderColumn(name = "position")
    private List<TransactionEntryJpaEntity> entries = new ArrayList<>();

    
    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name = "transaction_id", nullable = false)
    @OrderColumn(name = "position")
    private List<TransferJpaEntity> transfers = new ArrayList<>();
}
