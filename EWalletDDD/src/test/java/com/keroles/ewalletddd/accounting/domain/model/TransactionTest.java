package com.keroles.ewalletddd.accounting.domain.model;

import com.keroles.ewalletddd.accounting.domain.valueObject.AccountId;
import com.keroles.ewalletddd.accounting.domain.valueObject.AccountReference;
import com.keroles.ewalletddd.accounting.domain.valueObject.AccountType;
import com.keroles.ewalletddd.accounting.domain.valueObject.Party;
import com.keroles.ewalletddd.shared.domain.Money;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class TransactionTest {

    private final Party self = Party.internal(AccountReference.newRef(), AccountType.USER);
    private final Money amount = Money.of("40.00", "AED");
    private final Money balanceAfter = Money.of("60.00", "AED");

    @Test
    void withdrawalGoesFromUsToExternal() {
        Transaction tx = Transaction.withdrawal(new AccountId(1L), self, amount, balanceAfter);
        assertEquals(self, tx.sender());
        assertEquals(Party.EXTERNAL, tx.receiver());
        assertEquals(AccountType.EXTERNAL, tx.receiver().type());
    }

    @Test
    void depositComesFromExternalToUs() {
        Transaction tx = Transaction.deposit(new AccountId(1L), self, amount, balanceAfter);
        assertEquals(Party.EXTERNAL, tx.sender());
        assertEquals(self, tx.receiver());
    }
}
