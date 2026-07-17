package com.keroles.ewalletddd.accounting.domain.model;

import com.keroles.ewalletddd.accounting.domain.exception.InsufficientBalanceException;
import com.keroles.ewalletddd.accounting.domain.event.FundsHeldEvent;
import com.keroles.ewalletddd.accounting.domain.event.FundsReleasedEvent;
import com.keroles.ewalletddd.accounting.domain.event.FundsSettledEvent;
import com.keroles.ewalletddd.accounting.domain.event.MoneyDepositedEvent;
import com.keroles.ewalletddd.accounting.domain.event.MoneyWithdrawnEvent;
import com.keroles.ewalletddd.shared.domain.Money;
import com.keroles.ewalletddd.shared.domain.UserId;

import java.util.ArrayList;
import java.util.Currency;
import java.util.List;

/**
 * Aggregate root. One Account per user per currency. Pure Java — no framework.
 *
 * There is NO setBalance(). Only business verbs; balances change as a consequence.
 * Invariant: hold moves are atomic — main + hold total only changes on
 * deposit, withdraw and settle.
 *
 * Money leaving the wallet via cashout: hold() -> settle() on success | release() on failure.
 */
public class Account {

    private AccountId id; // null until first save — DB auto-increment births the identity
    private final UserId userId;
    private final Currency currency;
    private Money balance;      // main balance
    private Money holdBalance;  // reserved for in-flight transactions
    private final List<Object> events = new ArrayList<>();

    private Account(AccountId id, UserId userId, Currency currency, Money balance, Money holdBalance) {
        this.id = id;
        this.userId = userId;
        this.currency = currency;
        this.balance = balance;
        this.holdBalance = holdBalance;
    }

    public static Account open(UserId userId, Currency currency) {
        // AccountOpenedEvent is raised by the app service AFTER save — id doesn't exist yet here
        return new Account(null, userId, currency, Money.zero(currency), Money.zero(currency));
    }

    /** Called ONCE by the persistence adapter after INSERT. */
    public void assignId(AccountId id) {
        if (this.id != null) throw new IllegalStateException("Account already has id " + this.id.value());
        this.id = id;
    }

    /** Reconstitution from persistence — no business rules, no events. */
    public static Account restore(AccountId id, UserId userId, Currency currency, Money balance, Money holdBalance) {
        return new Account(id, userId, currency, balance, holdBalance);
    }

    public void deposit(Money amount) {
        assertCurrency(amount);
        balance = balance.add(amount);
        events.add(new MoneyDepositedEvent(id, amount, balance));
    }

    public void withdraw(Money amount) {
        assertCurrency(amount);
        assertSufficientBalance(amount);
        balance = balance.subtract(amount);
        events.add(new MoneyWithdrawnEvent(id, amount, balance));
    }

    /** Reserve money for an in-flight transaction: main -> hold. */
    public void hold(Money amount) {
        assertCurrency(amount);
        assertSufficientBalance(amount);
        balance = balance.subtract(amount);
        holdBalance = holdBalance.add(amount);
        events.add(new FundsHeldEvent(id, amount, balance, holdBalance));
    }

    /** Transaction succeeded: held money leaves the wallet (hold -> 0). */
    public void settle(Money amount) {
        assertCurrency(amount);
        holdBalance = holdBalance.subtract(amount); // Money VO rejects negative result
        events.add(new FundsSettledEvent(id, amount, balance, holdBalance));
    }

    /** Transaction failed: held money returns to main balance. */
    public void release(Money amount) {
        assertCurrency(amount);
        holdBalance = holdBalance.subtract(amount);
        balance = balance.add(amount);
        events.add(new FundsReleasedEvent(id, amount, balance, holdBalance));
    }

    /** Application layer pulls and publishes after save. Clears the buffer. */
    public List<Object> pullEvents() {
        List<Object> pulled = List.copyOf(events);
        events.clear();
        return pulled;
    }

    private void assertCurrency(Money amount) {
        if (!currency.equals(amount.currency()))
            throw new IllegalArgumentException(
                    "Account is " + currency + ", operation is " + amount.currency());
    }

    private void assertSufficientBalance(Money amount) {
        if (balance.isLessThan(amount))
            throw new InsufficientBalanceException(id, balance, amount);
    }

    public AccountId id() { return id; }
    public UserId userId() { return userId; }
    public Currency currency() { return currency; }
    public Money balance() { return balance; }
    public Money holdBalance() { return holdBalance; }
}
