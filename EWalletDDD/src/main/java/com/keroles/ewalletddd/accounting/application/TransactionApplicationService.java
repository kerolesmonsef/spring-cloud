package com.keroles.ewalletddd.accounting.application;

import com.keroles.ewalletddd.accounting.domain.model.Account;
import com.keroles.ewalletddd.accounting.domain.valueObject.AccountId;
import com.keroles.ewalletddd.accounting.domain.valueObject.AccountType;
import com.keroles.ewalletddd.accounting.domain.model.Transaction;
import com.keroles.ewalletddd.accounting.domain.valueObject.Party;
import com.keroles.ewalletddd.accounting.domain.valueObject.TransactionId;
import com.keroles.ewalletddd.accounting.domain.repository.AccountRepository;
import com.keroles.ewalletddd.accounting.domain.repository.TransactionRepository;
import com.keroles.ewalletddd.shared.domain.Currency;
import com.keroles.ewalletddd.shared.domain.Money;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

// Money movements on the ledger. Every movement is internal↔internal — the SYSTEM (house) account is one
// side of every user-facing flow. Each movement writes the Account balances AND its Transaction record in ONE tx.
//   topup    system -> user   (fund a wallet)          synchronous, one-shot
//   transfer user   -> user   (send money)             synchronous, one-shot
//   cashout  user   -> system (withdraw off the wallet) two-phase: hold now, settle/release on the rail outcome
// settle/release exist ONLY because cashout has an async rail gap; topup/transfer finish atomically and never use them.
@Service
public class TransactionApplicationService {

    private final AccountRepository accountRepository;
    private final TransactionRepository transactionRepository;
    private final ApplicationEventPublisher eventPublisher;

    public TransactionApplicationService(AccountRepository accounts,
                                         TransactionRepository transactions,
                                         ApplicationEventPublisher eventPublisher) {
        this.accountRepository = accounts;
        this.transactionRepository = transactions;
        this.eventPublisher = eventPublisher;
    }

    @Transactional
    public void topup(AccountId userAccountId, Money amount) {
        Account user = loadAccount(userAccountId);
        Account system = loadSystemAccount(user.currency());
        system.withdraw(amount);
        user.deposit(amount);

        Transaction tx = Transaction.start(Transaction.Type.TOPUP, partyOf(system), partyOf(user));
        tx.addEntry(system.id(), Transaction.Entry.Direction.DEBIT, amount, system.balance());
        tx.addEntry(user.id(), Transaction.Entry.Direction.CREDIT, amount, user.balance());
        tx.complete();

        saveAll(tx, system, user);
    }

    @Transactional
    public void transfer(AccountId fromId, AccountId toId, Money amount) {
        Account from = loadAccount(fromId);
        Account to = loadAccount(toId);
        from.withdraw(amount);
        to.deposit(amount);

        Transaction tx = Transaction.start(Transaction.Type.TRANSFER, partyOf(from), partyOf(to));
        tx.addEntry(from.id(), Transaction.Entry.Direction.DEBIT, amount, from.balance());
        tx.addEntry(to.id(), Transaction.Entry.Direction.CREDIT, amount, to.balance());
        tx.complete();

        saveAll(tx, from, to);
    }

    // user -> system, two-phase. Holds the funds; the money reaches system only on settle (success).
    @Transactional
    public TransactionId cashout(AccountId userAccountId, Money amount) {
        Account user = loadAccount(userAccountId);
        Account system = loadSystemAccount(user.currency()); // receiver — also fails fast if the house account is missing
        user.hold(amount);

        Transaction tx = Transaction.start(Transaction.Type.CASHOUT, partyOf(user), partyOf(system));
        tx.addEntry(user.id(), Transaction.Entry.Direction.DEBIT, amount, user.balance());

        accountRepository.save(user);
        transactionRepository.save(tx);
        publishEvents(user);
        return tx.id();
    }

    @Transactional
    public void settle(TransactionId txId) {
        Transaction tx = loadTransaction(txId);
        Transaction.Entry userDebit = singleEntryOf(tx); // the hold leg recorded at cashout
        if (tx.status() != Transaction.Status.PENDING)
            throw new IllegalStateException("Transaction is already " + tx.status());

        Account user = loadAccount(userDebit.accountId());
        user.settle(userDebit.amount());
        Account system = loadSystemAccount(user.currency());
        system.deposit(userDebit.amount());
        tx.addEntry(system.id(), Transaction.Entry.Direction.CREDIT, userDebit.amount(), system.balance());
        tx.complete();

        saveAll(tx, user, system);
    }

    @Transactional
    public void release(TransactionId txId) {
        Transaction tx = loadTransaction(txId);
        Transaction.Entry userDebit = singleEntryOf(tx);
        tx.fail();

        Account user = loadAccount(userDebit.accountId());
        user.release(userDebit.amount());

        accountRepository.save(user);
        transactionRepository.save(tx);
        publishEvents(user);
    }

    private Account loadSystemAccount(Currency currency) {
        return accountRepository.findByTypeAndCurrency(AccountType.SYSTEM, currency)
                .orElseThrow(() -> new IllegalStateException("No SYSTEM account for " + currency.code()
                        + " — seed one before moving money in this currency"));
    }

    private void saveAll(Transaction tx, Account a, Account b) {
        accountRepository.save(a);
        accountRepository.save(b);
        transactionRepository.save(tx);
        publishEvents(a);
        publishEvents(b);
    }

    private Party partyOf(Account account) {
        return Party.internal(account.reference(), account.type());
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
}
