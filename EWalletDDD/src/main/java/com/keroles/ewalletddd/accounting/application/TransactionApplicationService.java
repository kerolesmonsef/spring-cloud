package com.keroles.ewalletddd.accounting.application;

import com.keroles.ewalletddd.accounting.domain.model.Account;
import com.keroles.ewalletddd.accounting.domain.valueObject.AccountId;
import com.keroles.ewalletddd.accounting.domain.valueObject.AccountReference;
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

import java.util.UUID;

// Money movements on the ledger. Each movement writes the Account balances AND its Transaction record in ONE tx.
//   topup           system -> user   synchronous, one-shot
//   transfer        user   -> user   synchronous, one-shot
//   initiateTransfer user  -> user   two-phase: hold now, settle/release on the rail outcome
//   cashout         user   -> system two-phase: hold now, settle/release on the rail outcome
// settle/release resolve sender/receiver/amount from the Transaction header (not its entries) — the
// header is the source of truth; entries are per-account ledger postings (room for future fee/vat legs).
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

        Transaction tx = Transaction.start(Transaction.Type.TOPUP, partyOf(system), partyOf(user), amount);
        tx.addEntry(system.id(), Transaction.Entry.Direction.DEBIT, amount, system.balance());
        tx.addEntry(user.id(), Transaction.Entry.Direction.CREDIT, amount, user.balance());
        tx.complete();

        saveAll(tx, system, user);
    }

    // user -> user, synchronous. Moves money atomically, neither hold nor settle.
    @Transactional
    public void transfer(AccountId fromId, AccountId toId, Money amount) {
        Account from = loadAccount(fromId);
        Account to = loadAccount(toId);
        from.withdraw(amount);
        to.deposit(amount);

        Transaction tx = Transaction.start(Transaction.Type.TRANSFER, partyOf(from), partyOf(to), amount);
        tx.addEntry(from.id(), Transaction.Entry.Direction.DEBIT, amount, from.balance());
        tx.addEntry(to.id(), Transaction.Entry.Direction.CREDIT, amount, to.balance());
        tx.complete();

        saveAll(tx, from, to);
    }

    // user -> user, two-phase. Holds the sender's funds; receiver gets credited only on settle.
    @Transactional
    public TransactionId initiateTransfer(AccountId fromId, AccountId toId, Money amount) {
        Account from = loadAccount(fromId);
        Account to = loadAccount(toId);
        from.hold(amount);

        Transaction tx = Transaction.start(Transaction.Type.TRANSFER, partyOf(from), partyOf(to), amount);
        tx.addEntry(from.id(), Transaction.Entry.Direction.DEBIT, amount, from.balance());

        accountRepository.save(from);
        transactionRepository.save(tx);
        publishEvents(from);
        return tx.id();
    }

    // user -> system, two-phase. Holds the funds; the money reaches system only on settle (success).
    @Transactional
    public TransactionId cashout(AccountId userAccountId, Money amount) {
        Account userAccount = loadAccount(userAccountId);
        Account system = loadSystemAccount(userAccount.currency()); // receiver — also fails fast if the house account is missing
        userAccount.hold(amount);

        Transaction tx = Transaction.start(Transaction.Type.CASHOUT, partyOf(userAccount), partyOf(system), amount);
        tx.addEntry(userAccount.id(), Transaction.Entry.Direction.DEBIT, amount, userAccount.balance());

        accountRepository.save(userAccount);
        transactionRepository.save(tx);
        publishEvents(userAccount);
        return tx.id();
    }

    @Transactional
    public void settle(TransactionId txId) {
        Transaction tx = loadTransaction(txId);
        if (tx.status() != Transaction.Status.PENDING)
            throw new IllegalStateException("Transaction is already " + tx.status());
        if (tx.type() != Transaction.Type.CASHOUT && tx.type() != Transaction.Type.TRANSFER)
            throw new IllegalArgumentException("Cannot settle " + tx.type() + " transaction");

        Account sender = resolveParty(tx.sender());
        sender.settle(tx.amount());
        Account receiver = resolveParty(tx.receiver());
        receiver.deposit(tx.amount());
        tx.addEntry(receiver.id(), Transaction.Entry.Direction.CREDIT, tx.amount(), receiver.balance());
        tx.complete();

        saveAll(tx, sender, receiver);
    }

    @Transactional
    public void release(TransactionId txId) {
        Transaction tx = loadTransaction(txId);
        if (tx.type() != Transaction.Type.CASHOUT && tx.type() != Transaction.Type.TRANSFER)
            throw new IllegalArgumentException("Cannot release " + tx.type() + " transaction");

        tx.fail();

        Account holder = resolveParty(tx.sender());
        holder.release(tx.amount());

        accountRepository.save(holder);
        transactionRepository.save(tx);
        publishEvents(holder);
    }

    // Resolves an internal Party (sender or receiver) back to the Account it names — the Transaction
    // header, not its entries, is the source of truth for who's involved and how much moved.
    private Account resolveParty(Party party) {
        AccountReference ref = new AccountReference(UUID.fromString(party.reference()));
        return accountRepository.findByReference(ref)
                .orElseThrow(() -> new IllegalArgumentException("No account for party: " + party.reference()));
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

    private void publishEvents(Account account) {
        account.pullEvents().forEach(eventPublisher::publishEvent);
    }
}
