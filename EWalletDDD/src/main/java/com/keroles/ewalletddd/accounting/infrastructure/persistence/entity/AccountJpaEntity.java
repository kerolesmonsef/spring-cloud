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

    @JdbcTypeCode(SqlTypes.CHAR)   // char(36) — readable in SQL; stable public handle, unlike the DB id
    @Column(nullable = false, unique = true, updatable = false, length = 36)
    private UUID accountReference;

    /** FK to users. Association exists ONLY so DDL creates the constraint; never navigated. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private UserJpaEntity user;

    /** Read-side mirror of the FK column — mapping reads this, not the lazy proxy. */
    @Column(name = "user_id", nullable = false, insertable = false, updatable = false)
    private Long userId;

    @Column(nullable = false, length = 3)
    private String currency; // ISO code — the domain's source of truth; a_currencies FK below mirrors it for integrity

    /** Link to a_currencies. Set on save, never navigated (mapper reads the code column above). */
    // ponytail: FK nullable — ddl-auto=update can't back-fill it on pre-existing a_accounts rows; new accounts always set it
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "currency_id")
    private CurrencyJpaEntity currencyRef;

    @Column(name = "account_type", length = 20)
    private String accountType; // enum name, lowercased (matches a_account_types.name); null on legacy rows -> USER

    /** Link to a_account_types. Set on save, never navigated (mapper reads the account_type column above). */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "account_type_id")
    private AccountTypeJpaEntity accountTypeRef;

    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal balance;

    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal holdBalance;

    /** Optimistic lock: two racing updates -> second one throws instead of silently clobbering the ledger. */
    @Version
    private Long version;
}
