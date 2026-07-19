package com.keroles.ewalletddd.accounting.infrastructure.reference;

import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

// context startup already ran the seeder once; run() again proves idempotency (the "check if exists" requirement)
@SpringBootTest
@RequiredArgsConstructor
class ReferenceDataSeederIT {

    private final ReferenceDataSeeder seeder;
    private final AccountTypeRepository accountTypes;
    private final CurrencyRepository currencies;

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
}
