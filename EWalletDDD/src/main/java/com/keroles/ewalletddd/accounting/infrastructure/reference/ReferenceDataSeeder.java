package com.keroles.ewalletddd.accounting.infrastructure.reference;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

// Seeds accounting reference data on startup. Idempotent: existence-checked, safe to re-run.
@Component
public class ReferenceDataSeeder implements CommandLineRunner {

    private final AccountTypeRepository accountTypes;
    private final CurrencyRepository currencies;
    private final String defaultCurrency;

    public ReferenceDataSeeder(AccountTypeRepository accountTypes,
                               CurrencyRepository currencies,
                               @Value("${default.currency:AED}") String defaultCurrency) {
        this.accountTypes = accountTypes;
        this.currencies = currencies;
        this.defaultCurrency = defaultCurrency;
    }

    @Override
    public void run(String... args) {
        seedType("system");
        seedType("user");

        seedCurrency(defaultCurrency);
        seedCurrency("ETH");
        seedCurrency("SOL");
        seedCurrency("BTC");
    }

    private void seedType(String name) {
        if (!accountTypes.existsByName(name))
            accountTypes.save(new AccountTypeJpaEntity(name));
    }

    private void seedCurrency(String currencyCode) {
        if (!currencies.existsByCode(currencyCode))
            currencies.save(new CurrencyJpaEntity(currencyCode));
    }
}
