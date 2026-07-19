package com.keroles.ewalletddd.accounting.infrastructure.reference;

import com.keroles.ewalletddd.accounting.domain.model.Account;
import com.keroles.ewalletddd.accounting.domain.repository.AccountRepository;
import com.keroles.ewalletddd.accounting.domain.valueObject.AccountType;
import com.keroles.ewalletddd.accounting.infrastructure.persistence.repository.SpringDataAccountJpa;
import com.keroles.ewalletddd.shared.domain.Currency;
import com.keroles.ewalletddd.shared.domain.Money;
import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

// context startup already ran the seeder once; run() again proves idempotency (the "check if exists" requirement)
@SpringBootTest
@RequiredArgsConstructor
class ReferenceDataSeederIT {

    private final ReferenceDataSeeder seeder;
    private final AccountTypeRepository accountTypes;
    private final CurrencyRepository currencies;
    private final AccountRepository accounts;
    private final SpringDataAccountJpa accountJpa;

    @Test
    void seedsExistAndAreNotDuplicatedOnRerun() {
        seeder.run();

        assertTrue(accountTypes.existsByName("system"));
        assertTrue(accountTypes.existsByName("user"));
        assertTrue(currencies.existsByCode("AED"));
        assertTrue(currencies.existsByCode("SOL"));
        assertTrue(currencies.existsByCode("BTC"));
        assertTrue(currencies.existsByCode("ETH"));


        assertEquals(2, accountTypes.count());
        assertEquals(4, currencies.count());
    }

    @Test
    void systemAccountsSeededOncePerCurrencyFundedWith1000() {
        seeder.run(); // idempotent re-run

        for (String code : List.of("AED", "ETH", "SOL", "BTC"))
            assertTrue(accounts.existsByTypeAndCurrency(AccountType.SYSTEM, Currency.of(code)),
                    "system account missing for " + code);

        assertEquals(4, accountJpa.countByAccountType("system")); // one per currency, no dupes after re-run

        Account systemAccount = accounts.findFirstByType(AccountType.SYSTEM).orElseThrow();
        assertEquals(new Money(new BigDecimal("1000"), systemAccount.currency()), systemAccount.balance());
    }
}
