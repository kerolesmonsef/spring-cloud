package com.keroles.ewalletddd.accounting.application;

import com.keroles.ewalletddd.accounting.domain.event.AccountOpenedEvent;
import com.keroles.ewalletddd.accounting.domain.model.Account;
import com.keroles.ewalletddd.accounting.domain.valueObject.AccountId;
import com.keroles.ewalletddd.accounting.domain.model.Transaction;
import com.keroles.ewalletddd.accounting.domain.valueObject.TransactionId;
import com.keroles.ewalletddd.accounting.domain.model.User;
import com.keroles.ewalletddd.accounting.domain.repository.AccountRepository;
import com.keroles.ewalletddd.accounting.domain.repository.TransactionRepository;
import com.keroles.ewalletddd.accounting.domain.repository.UserRepository;
import com.keroles.ewalletddd.shared.domain.Money;
import com.keroles.ewalletddd.shared.domain.UserId;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Currency;

@Service
public class AccountApplicationService {

    private final AccountRepository accountRepository;
    private final TransactionRepository transactionRepository;
    private final UserRepository userRepository;
    private final ApplicationEventPublisher eventPublisher; // ponytail: in-process events; Outbox pattern when we need reliability across services

    public AccountApplicationService(AccountRepository accounts,
                                     TransactionRepository transactions,
                                     UserRepository users,
                                     ApplicationEventPublisher eventPublisher) {
        this.accountRepository = accounts;
        this.transactionRepository = transactions;
        this.userRepository = users;
        this.eventPublisher = eventPublisher;
    }

    @Transactional
    public AccountId openAccount(UserId userId, Currency currency) {
        UserId owner = (userId == null) ? registerUser() : existingUser(userId);
        accountRepository.findByUserAndCurrency(owner, currency).ifPresent(existing -> {
            throw new IllegalStateException("User already has a " + currency + " account"); // TODO is this legal to throw here ?
        });
        Account account = Account.open(owner, currency);
        accountRepository.save(account); // adapter assigns the auto-increment id back onto the aggregate
        // raised here, not in Account.open(): the id is born in the DB, so the event can't exist before save
        eventPublisher.publishEvent(new AccountOpenedEvent(account.id(), owner, currency));
        publishEvents(account);
        return account.id();
    }



    @Transactional
    public void deposit(AccountId accountId, Money amount) {
        Account account = loadAccount(accountId);
        account.deposit(amount);

        Transaction tx = Transaction.deposit(accountId, amount, account.balance());

        accountRepository.save(account);
        transactionRepository.save(tx);
        publishEvents(account);
    }

    @Transactional
    public void withdraw(AccountId accountId, Money amount) {
        Account account = loadAccount(accountId);
        account.withdraw(amount);

        Transaction tx = Transaction.withdrawal(accountId, amount, account.balance());

        accountRepository.save(account);
        transactionRepository.save(tx);

        publishEvents(account);
    }

    // balance check + hold run in one DB tx — two concurrent reserves can't both pass
    @Transactional
    public TransactionId reserve(AccountId accountId, Money amount) {
        Account account = loadAccount(accountId);
        account.hold(amount);

        Transaction tx = Transaction.start(Transaction.Type.CASHOUT);
        tx.addEntry(accountId, Transaction.Entry.Direction.DEBIT, amount, account.balance());

        accountRepository.save(account);
        transactionRepository.save(tx);
        publishEvents(account);
        return tx.id();
    }

    @Transactional
    public void settle(TransactionId txId) {
        Transaction tx = loadTransaction(txId);
        Transaction.Entry entry = singleEntryOf(tx);
        tx.complete(); // throws if not PENDING — duplicate settle is rejected here

        Account account = loadAccount(entry.accountId());
        account.settle(entry.amount());

        accountRepository.save(account);
        transactionRepository.save(tx);
        publishEvents(account);
    }

    @Transactional
    public void release(TransactionId txId) {
        Transaction tx = loadTransaction(txId);
        Transaction.Entry entry = singleEntryOf(tx);
        tx.fail();

        Account account = loadAccount(entry.accountId());
        account.release(entry.amount());

        accountRepository.save(account);
        transactionRepository.save(tx);
        publishEvents(account);
    }

    @Transactional(readOnly = true)
    public Account getAccount(AccountId accountId) {
        return loadAccount(accountId);
    }

    @Transactional(readOnly = true)
    public java.util.List<Account> getUserAccounts(UserId userId) {
        return accountRepository.findByUser(userId);
    }

    private Account loadAccount(AccountId id) {
        return accountRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("No account " + id.value()));
    }

    private Transaction loadTransaction(TransactionId id) {
        return transactionRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("No transaction " + id.value()));
    }

    private Transaction.Entry singleEntryOf(Transaction tx) {
        if (tx.entries().size() != 1)
            throw new IllegalStateException("Expected single-entry transaction: " + tx.id().value());
        return tx.entries().get(0);
    }

    private void publishEvents(Account account) {
        account.pullEvents().forEach(eventPublisher::publishEvent);
    }

    private UserId registerUser() {
        User user = User.register();
        userRepository.save(user);
        return user.id();
    }

    private UserId existingUser(UserId userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("No user " + userId.value()))
                .id();
    }
}
