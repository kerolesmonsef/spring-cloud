package com.keroles.ewalletddd.accounting.infrastructure.reference;

import com.keroles.ewalletddd.accounting.infrastructure.persistence.entity.AccountJpaEntity;
import com.keroles.ewalletddd.accounting.infrastructure.persistence.entity.UserJpaEntity;
import com.keroles.ewalletddd.accounting.infrastructure.persistence.repository.SpringDataAccountJpa;
import com.keroles.ewalletddd.accounting.infrastructure.persistence.repository.SpringDataUserJpa;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

// Seeds accounting reference data + house accounts on startup. Repo-only (no domain aggregate), existence-checked/idempotent.
@Component
public class ReferenceDataSeeder implements CommandLineRunner {

    // Large genesis: topup draws user funding from here, and (MySQL) ITs commit + the seed is idempotent
    // (never refilled), so a small float would deplete across runs -> spurious InsufficientBalanceException.
    private static final BigDecimal SYSTEM_ACCOUNT_SEED = new BigDecimal("1000000000");
    private static final String SYSTEM_TYPE = "system";

    private final AccountTypeRepository accountTypes;
    private final CurrencyRepository currencies;
    private final SpringDataAccountJpa accounts;
    private final SpringDataUserJpa users;
    private final String defaultCurrency;

    public ReferenceDataSeeder(AccountTypeRepository accountTypes,
                               CurrencyRepository currencies,
                               SpringDataAccountJpa accounts,
                               SpringDataUserJpa users,
                               @Value("${default.currency:AED}") String defaultCurrency) {
        this.accountTypes = accountTypes;
        this.currencies = currencies;
        this.accounts = accounts;
        this.users = users;
        this.defaultCurrency = defaultCurrency;
    }

    // one tx so the get-or-create'd type/currency rows stay managed for the account inserts below
    @Override
    @Transactional
    public void run(String... args) {
        AccountTypeJpaEntity systemType = seedType(SYSTEM_TYPE);
        seedType("user");

        List<String> supported = List.of(defaultCurrency, "ETH", "SOL", "BTC");
        UserJpaEntity systemUser = systemUser(); // one shared owner for all house accounts
        for (String code : supported) {
            CurrencyJpaEntity currency = seedCurrency(code);
            ensureSystemAccount(systemType, currency, systemUser); // one per currency, funded 1000
        }
    }

    // check-if-exists then create — type=SYSTEM + currency is the uniqueness key
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

    // reuse the owner of any existing system account (re-runs add no orphan users); else create one
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

