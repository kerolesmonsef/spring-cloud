package com.keroles.ewalletddd.pricing.infrastructure.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "p_fee_charges")
@Getter
@Setter
@NoArgsConstructor
public class FeeChargeJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 20)
    private String transactionType;

    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal senderFee;

    @Column(nullable = false, length = 10)
    private String senderFeeType;

    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal receiverFee;

    @Column(nullable = false, length = 10)
    private String receiverFeeType;

    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal vatPercentage;

    @Column(nullable = false)
    private Instant createdAt;
}
