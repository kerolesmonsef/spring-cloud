package com.keroles.ewalletddd.accounting.application;

import com.keroles.ewalletddd.accounting.domain.event.AccountOpenedEvent;
import com.keroles.ewalletddd.accounting.domain.model.Account;
import com.keroles.ewalletddd.accounting.domain.model.AccountId;
import com.keroles.ewalletddd.accounting.domain.model.Transaction;
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

/**
 * The FRONT DOOR of the Accounting (Ledger) context. Other contexts (Cashout's
 * LedgerAccountPort adapter, web controllers) call ONLY this — never repositories,
 * never aggregates directly.
 *
 * No business rules here: load aggregate -> business verb -> save -> publish events.
 * Each method is one whole, transactional use case.
 */
@Service
public class AccountApplicationService {

    private final AccountRepository accounts;
    private final TransactionRepository transactions;
    private final UserRepository users;
    private final ApplicationEventPublisher eventPublisher; // ponytail: in-process events; Outbox pattern when we need reliability across services

    public AccountApplicationService(AccountRepository accounts,
                                     TransactionRepository transactions,
                                     UserRepository users,
                                     ApplicationEventPublisher eventPublisher) {
        this.accounts = accounts;
        this.transactions = transactions;
        this.users = users;
        this.eventPublisher = eventPublisher;
    }

    /**
     * userId null -> register a brand-new user and open their first account.
     * userId given -> user must exist (accounts.user_id is a FK to users).
     */
    @Transactional
    public AccountId openAccount(UserId userId, Currency currency) {
        UserId owner = (userId == null) ? registerUser() : existingUser(userId);
        accounts.findByUserAndCurrency(owner, currency).ifPresent(existing -> {
            throw new IllegalStateException("User already has a " + currency + " account"); // TODO is this legal to throw here ?
        });
        Account account = Account.open(owner, currency);
        accounts.save(account); // adapter assigns the auto-increment id back onto the aggregate
        // raised here, not in Account.open(): the id is born in the DB, so the event can't exist before save
        eventPublisher.publishEvent(new AccountOpenedEvent(account.id(), owner, currency));
        publishEvents(account);
        return account.id();
    }

    private UserId registerUser() {
        User user = User.register();
        users.save(user);
        return user.id();
    }

    private UserId existingUser(UserId userId) {
        return users.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("No user " + userId.value()))
                .id();
    }

    @Transactional
    public void deposit(AccountId accountId, Money amount) {
        Account account = loadAccount(accountId);
        account.deposit(amount);

        Transaction tx = Transaction.start(Transaction.Type.DEPOSIT);
        tx.addEntry(accountId, Transaction.Entry.Direction.CREDIT, amount, account.balance());
        tx.complete();

        accounts.save(account);
        transactions.save(tx);
        publishEvents(account);
    }

    @Transactional
    public void withdraw(AccountId accountId, Money amount) {
        Account account = loadAccount(accountId);
        account.withdraw(amount);

        Transaction tx = Transaction.start(Transaction.Type.WITHDRAWAL);
        tx.addEntry(accountId, Transaction.Entry.Direction.DEBIT, amount, account.balance());
        tx.complete();

        accounts.save(account);
        transactions.save(tx);
        publishEvents(account);
    }

    /**
     * Reserve funds for an in-flight operation (e.g. cashout): main -> hold,
     * plus a PENDING ledger transaction. Balance check + hold are one DB
     * transaction — two concurrent reserves cannot both pass the check.
     */
    @Transactional
    public Transaction.TransactionId reserve(AccountId accountId, Money amount) {
        Account account = loadAccount(accountId);
        account.hold(amount);

        Transaction tx = Transaction.start(Transaction.Type.CASHOUT);
        tx.addEntry(accountId, Transaction.Entry.Direction.DEBIT, amount, account.balance());

        accounts.save(account);
        transactions.save(tx);
        publishEvents(account);
        return tx.id();
    }

    /** Operation succeeded: hold -> gone, PENDING -> COMPLETED. Idempotent via Transaction's status guard. */
    @Transactional
    public void settle(Transaction.TransactionId txId) {
        Transaction tx = loadTransaction(txId);
        Transaction.Entry entry = singleEntryOf(tx);
        tx.complete(); // throws if not PENDING — duplicate settle is rejected here

        Account account = loadAccount(entry.accountId());
        account.settle(entry.amount());

        accounts.save(account);
        transactions.save(tx);
        publishEvents(account);
    }

    /** Operation failed: hold -> back to main, PENDING -> FAILED. */
    @Transactional
    public void release(Transaction.TransactionId txId) {
        Transaction tx = loadTransaction(txId);
        Transaction.Entry entry = singleEntryOf(tx);
        tx.fail();

        Account account = loadAccount(entry.accountId());
        account.release(entry.amount());

        accounts.save(account);
        transactions.save(tx);
        publishEvents(account);
    }

    @Transactional(readOnly = true)
    public Account getAccount(AccountId accountId) {
        return loadAccount(accountId);
    }

    @Transactional(readOnly = true)
    public java.util.List<Account> getUserAccounts(UserId userId) {
        return accounts.findByUser(userId);
    }

    private Account loadAccount(AccountId id) {
        return accounts.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("No account " + id.value()));
    }

    private Transaction loadTransaction(Transaction.TransactionId id) {
        return transactions.findById(id)
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
}
