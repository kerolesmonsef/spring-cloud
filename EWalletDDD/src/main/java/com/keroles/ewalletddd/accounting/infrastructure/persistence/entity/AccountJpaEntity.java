package com.keroles.ewalletddd.accounting.infrastructure.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import com.keroles.ewalletddd.accounting.infrastructure.reference.AccountTypeJpaEntity;
import com.keroles.ewalletddd.accounting.infrastructure.reference.CurrencyJpaEntity;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "a_accounts")
@Getter
@Setter
@NoArgsConstructor
public class AccountJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @JdbcTypeCode(SqlTypes.CHAR)   
    @Column(nullable = false, unique = true, updatable = false, length = 36)
    private UUID accountReference;

    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private UserJpaEntity user;

    
    @Column(name = "user_id", nullable = false, insertable = false, updatable = false)
    private Long userId;

    @Column(nullable = false, length = 3)
    private String currency; 

    
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "currency_id")
    private CurrencyJpaEntity currencyRef;

    @Column(name = "account_type", length = 20)
    private String accountType; 

    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "account_type_id")
    private AccountTypeJpaEntity accountTypeRef;

    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal balance;

    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal holdBalance;

    
    @Version
    private Long version;
}
