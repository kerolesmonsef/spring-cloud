package com.keroles.ewalletddd.accounting.domain.model;

import com.keroles.ewalletddd.accounting.domain.exception.InsufficientBalanceException;
import com.keroles.ewalletddd.accounting.domain.event.FundsHeldEvent;
import com.keroles.ewalletddd.accounting.domain.event.FundsReleasedEvent;
import com.keroles.ewalletddd.accounting.domain.event.FundsSettledEvent;
import com.keroles.ewalletddd.accounting.domain.event.MoneyDepositedEvent;
import com.keroles.ewalletddd.accounting.domain.event.MoneyWithdrawnEvent;
import com.keroles.ewalletddd.accounting.domain.valueObject.AccountId;
import com.keroles.ewalletddd.accounting.domain.valueObject.AccountReference;
import com.keroles.ewalletddd.accounting.domain.valueObject.AccountType;
import com.keroles.ewalletddd.shared.domain.Currency;
import com.keroles.ewalletddd.shared.domain.Money;
import com.keroles.ewalletddd.shared.domain.UserId;

import java.util.ArrayList;
import java.util.List;

public class Account {

    private AccountId id; 
    private final AccountReference reference; 
    private final UserId userId;
    private final Currency currency;
    private final AccountType type;
    private Money balance;
    private Money holdBalance;  
    private final List<Object> events = new ArrayList<>();

    private Account(AccountId id, AccountReference reference, UserId userId, Currency currency, AccountType type, Money balance, Money holdBalance) {
        this.id = id;
        this.reference = reference;
        this.userId = userId;
        this.currency = currency;
        this.type = type;
        this.balance = balance;
        this.holdBalance = holdBalance;
    }

    public static Account open(UserId userId, Currency currency) {
        
        return new Account(null, AccountReference.newRef(), userId, currency, AccountType.USER, Money.zero(currency), Money.zero(currency));
    }

    public void assignId(AccountId id) {
        if (this.id != null) throw new IllegalStateException("Account already has id " + this.id.value());
        this.id = id;
    }

    public static Account restore(AccountId id, AccountReference reference, UserId userId, Currency currency, AccountType type, Money balance, Money holdBalance) {
        return new Account(id, reference, userId, currency, type, balance, holdBalance);
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

    public void hold(Money amount) {
        assertCurrency(amount);
        assertSufficientBalance(amount);
        balance = balance.subtract(amount);
        holdBalance = holdBalance.add(amount);
        events.add(new FundsHeldEvent(id, amount, balance, holdBalance));
    }

    public void settle(Money amount) {
        assertCurrency(amount);
        holdBalance = holdBalance.subtract(amount); 
        events.add(new FundsSettledEvent(id, amount, balance, holdBalance));
    }

    public void release(Money amount) {
        assertCurrency(amount);
        holdBalance = holdBalance.subtract(amount);
        balance = balance.add(amount);
        events.add(new FundsReleasedEvent(id, amount, balance, holdBalance));
    }

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
    public AccountReference reference() { return reference; }
    public UserId userId() { return userId; }
    public Currency currency() { return currency; }
    public AccountType type() { return type; }
    public Money balance() { return balance; }
    public Money holdBalance() { return holdBalance; }
}
