package com.keroles.ewalletddd.accounting.domain.model;

import com.keroles.ewalletddd.accounting.domain.exception.InsufficientBalanceException;
import com.keroles.ewalletddd.shared.domain.Money;
import com.keroles.ewalletddd.shared.domain.UserId;
import org.junit.jupiter.api.Test;

import com.keroles.ewalletddd.shared.domain.Currency;

import static org.junit.jupiter.api.Assertions.*;

class AccountTest {

    private final Currency AED = Currency.of("AED");

    private Account accountWith(String amount) {
        Account a = Account.open(new UserId(1L), AED);
        a.deposit(Money.of(amount, "AED"));
        return a;
    }

    @Test
    void openAssignsARandomReference() {
        Account a = Account.open(new UserId(1L), AED);
        Account b = Account.open(new UserId(1L), AED);
        assertNotNull(a.reference().value());
        assertNotEquals(a.reference(), b.reference()); 
    }

    @Test
    void holdMovesMoneyFromMainToHold() {
        Account a = accountWith("100.00");
        a.hold(Money.of("40.00", "AED"));
        assertEquals(Money.of("60.00", "AED"), a.balance());
        assertEquals(Money.of("40.00", "AED"), a.holdBalance());
    }

    @Test
    void settleRemovesHeldMoney_success_path() {
        Account a = accountWith("100.00");
        a.hold(Money.of("40.00", "AED"));
        a.settle(Money.of("40.00", "AED"));
        assertEquals(Money.of("60.00", "AED"), a.balance());
        assertEquals(Money.zero(AED), a.holdBalance());
    }

    @Test
    void releaseReturnsMoneyToMain_failure_path() {
        Account a = accountWith("100.00");
        a.hold(Money.of("40.00", "AED"));
        a.release(Money.of("40.00", "AED"));
        assertEquals(Money.of("100.00", "AED"), a.balance());
        assertEquals(Money.zero(AED), a.holdBalance());
    }

    @Test
    void cannotHoldMoreThanBalance() {
        Account a = accountWith("10.00");
        assertThrows(InsufficientBalanceException.class, () -> a.hold(Money.of("10.01", "AED")));
    }

    @Test
    void rejectsForeignCurrency() {
        Account a = accountWith("10.00");
        assertThrows(IllegalArgumentException.class, () -> a.deposit(Money.of("5.00", "USD")));
    }

    @Test
    void businessVerbsRaiseDomainEvents() {
        Account a = accountWith("100.00");
        a.hold(Money.of("40.00", "AED"));
        assertEquals(2, a.pullEvents().size()); 
        assertTrue(a.pullEvents().isEmpty());   
    }
}
