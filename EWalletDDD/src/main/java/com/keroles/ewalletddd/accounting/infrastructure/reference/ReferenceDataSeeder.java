package com.keroles.ewalletddd.accounting.infrastructure.reference;

import com.keroles.ewalletddd.accounting.infrastructure.persistence.entity.AccountJpaEntity;
import com.keroles.ewalletddd.accounting.infrastructure.persistence.entity.UserJpaEntity;
import com.keroles.ewalletddd.accounting.infrastructure.persistence.repository.SpringDataAccountJpa;
import com.keroles.ewalletddd.accounting.infrastructure.persistence.repository.SpringDataUserJpa;
import com.keroles.ewalletddd.pricing.infrastructure.persistence.entity.FeeChargeJpaEntity;
import com.keroles.ewalletddd.pricing.infrastructure.persistence.repository.SpringDataFeeChargeJpa;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;


@Component
public class ReferenceDataSeeder implements CommandLineRunner {

    
    
    private static final BigDecimal SYSTEM_ACCOUNT_SEED = new BigDecimal("1000000000");
    private static final String SYSTEM_TYPE = "system";

    private final AccountTypeRepository accountTypes;
    private final CurrencyRepository currencies;
    private final SpringDataAccountJpa accounts;
    private final SpringDataUserJpa users;
    private final SpringDataFeeChargeJpa feeCharges;
    private final String defaultCurrency;

    public ReferenceDataSeeder(AccountTypeRepository accountTypes,
                               CurrencyRepository currencies,
                               SpringDataAccountJpa accounts,
                               SpringDataUserJpa users,
                               SpringDataFeeChargeJpa feeCharges,
                               @Value("${default.currency:AED}") String defaultCurrency) {
        this.accountTypes = accountTypes;
        this.currencies = currencies;
        this.accounts = accounts;
        this.users = users;
        this.feeCharges = feeCharges;
        this.defaultCurrency = defaultCurrency;
    }

    
    @Override
    @Transactional
    public void run(String... args) {
        AccountTypeJpaEntity systemType = seedType(SYSTEM_TYPE);
        seedType("user");

        List<String> supported = List.of(defaultCurrency, "ETH", "SOL", "BTC");
        UserJpaEntity systemUser = systemUser();
        for (String code : supported) {
            CurrencyJpaEntity currency = seedCurrency(code);
            ensureSystemAccount(systemType, currency, systemUser);
        }

        seedFeeCharge("TOPUP", new BigDecimal("1.50"), "PERCENTAGE", new BigDecimal("1.00"), "VALUE", new BigDecimal("5.00"));
        seedFeeCharge("CASHOUT", new BigDecimal("5.00"), "VALUE", new BigDecimal("0.50"), "PERCENTAGE", new BigDecimal("5.00"));
        seedFeeCharge("TRANSFER", new BigDecimal("1.00"), "PERCENTAGE", new BigDecimal("0.00"), "VALUE", new BigDecimal("5.00"));
    }


    private void seedFeeCharge(String transactionType, BigDecimal senderFee, String senderFeeType,
                                BigDecimal receiverFee, String receiverFeeType, BigDecimal vatPercentage) {
        if (feeCharges.existsByTransactionType(transactionType)) return;
        FeeChargeJpaEntity row = new FeeChargeJpaEntity();
        row.setTransactionType(transactionType);
        row.setSenderFee(senderFee);
        row.setSenderFeeType(senderFeeType);
        row.setReceiverFee(receiverFee);
        row.setReceiverFeeType(receiverFeeType);
        row.setVatPercentage(vatPercentage);
        row.setCreatedAt(Instant.now());
        feeCharges.save(row);
    }

    
    private void ensureSystemAccount(AccountTypeJpaEntity systemType, CurrencyJpaEntity currency, UserJpaEntity owner) {
        if (accounts.existsByAccountTypeAndCurrency(SYSTEM_TYPE, currency.getCode())) return;
        AccountJpaEntity row = new AccountJpaEntity();
        row.setAccountReference(UUID.randomUUID());
        row.setUser(owner);
        row.setCurrency(currency.getCode());
        row.setCurrencyRef(currency);
        row.setAccountType(SYSTEM_TYPE);
        row.setAccountTypeRef(systemType);
        row.setBalance(SYSTEM_ACCOUNT_SEED);
        row.setHoldBalance(BigDecimal.ZERO);
        accounts.save(row);
    }

    
    private UserJpaEntity systemUser() {
        return accounts.findFirstByAccountType(SYSTEM_TYPE)
                .map(a -> users.getReferenceById(a.getUserId()))
                .orElseGet(() -> users.save(new UserJpaEntity().setCreatedAt(Instant.now())));
    }

    private AccountTypeJpaEntity seedType(String name) {
        return accountTypes.findByName(name).orElseGet(() -> accountTypes.save(new AccountTypeJpaEntity(name)));
    }

    private CurrencyJpaEntity seedCurrency(String code) {
        return currencies.findByCode(code).orElseGet(() -> currencies.save(new CurrencyJpaEntity(code)));
    }
}

