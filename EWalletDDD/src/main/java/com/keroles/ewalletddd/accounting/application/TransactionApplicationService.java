package com.keroles.ewalletddd.accounting.application;

import com.keroles.ewalletddd.accounting.domain.model.Account;
import com.keroles.ewalletddd.accounting.domain.valueObject.AccountId;
import com.keroles.ewalletddd.accounting.domain.model.Transaction;
import com.keroles.ewalletddd.accounting.domain.valueObject.Party;
import com.keroles.ewalletddd.accounting.domain.valueObject.TransactionId;
import com.keroles.ewalletddd.accounting.domain.repository.AccountRepository;
import com.keroles.ewalletddd.accounting.domain.repository.TransactionRepository;
import com.keroles.ewalletddd.shared.domain.Money;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

// Money movements on the ledger. Each writes the Account balance AND its Transaction record in ONE tx.
// Split from AccountApplicationService (account lifecycle) so cross-context consumers (cashout's ACL)
// depend only on the movement surface, never on openAccount.
@Service
public class TransactionApplicationService {

    private final AccountRepository accountRepository;
    private final TransactionRepository transactionRepository;
    private final ApplicationEventPublisher eventPublisher; // ponytail: in-process events; Outbox pattern when we need reliability across services

    public TransactionApplicationService(AccountRepository accounts,
                                         TransactionRepository transactions,
                                         ApplicationEventPublisher eventPublisher) {
        this.accountRepository = accounts;
        this.transactionRepository = transactions;
        this.eventPublisher = eventPublisher;
    }

    @Transactional
    public void deposit(AccountId accountId, Money amount) {
        Account account = loadAccount(accountId);
        account.deposit(amount);

        Transaction tx = Transaction.deposit(accountId, partyOf(account), amount, account.balance());

        accountRepository.save(account);
        transactionRepository.save(tx);
        publishEvents(account);
    }

    @Transactional
    public void withdraw(AccountId accountId, Money amount) {
        Account account = loadAccount(accountId);
        account.withdraw(amount);

        Transaction tx = Transaction.withdrawal(accountId, partyOf(account), amount, account.balance());

        accountRepository.save(account);
        transactionRepository.save(tx);

        publishEvents(account);
    }

    // balance check + hold run in one DB tx — two concurrent reserves can't both pass
    @Transactional
    public TransactionId reserve(AccountId accountId, Money amount) {
        Account account = loadAccount(accountId);
        account.hold(amount);

        // ponytail: receiver is the EXTERNAL sentinel; cashout threads the real destination into reserve() next
        Transaction tx = Transaction.start(Transaction.Type.CASHOUT, partyOf(account), Party.EXTERNAL);
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

    private Party partyOf(Account account) {
        return Party.internal(account.reference(), account.type());
    }

    // ponytail: loadAccount + publishEvents also live in AccountApplicationService — 6 lines duplicated,
    // not worth a shared base class
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
