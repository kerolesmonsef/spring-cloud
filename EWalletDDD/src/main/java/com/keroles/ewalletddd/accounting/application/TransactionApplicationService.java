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
//   topup    system -> user   synchronous, one-shot
//   transfer user   -> user   two-phase: HOLD now, SETTLE/RELEASE resolves it later
//   cashout  user   -> system two-phase: HOLD now, SETTLE/RELEASE resolves it later
// Two-phase flows are TWO Transaction rows, not one mutated in place: the HOLD row is immutable once
// written (just its DEBIT entry); settle()/release() write a SEPARATE child row (Stage SETTLE/RELEASE)
// carrying the resolving entry, sharing parentCorrelationId with the HOLD so the pair/order is queryable
// by one value. sender/receiver/amount always come from the Transaction header, never its entries —
// entries are per-account ledger postings (room for future fee/vat legs).
// settle()/release() are generic over Transaction.Type: they resolve whichever hold the header names
// (sender/receiver/type), so every two-phase flow — transfer, cashout, and any future type — shares them.
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
    public TransactionId topup(AccountId userAccountId, Money amount) {
        Account user = loadAccount(userAccountId);
        Account system = loadSystemAccount(user.currency());
        system.withdraw(amount);
        user.deposit(amount);

        Transaction tx = Transaction.start(Transaction.Type.TOPUP, partyOf(system), partyOf(user), amount);
        tx.addEntry(system.id(), Transaction.Entry.Direction.DEBIT, amount, system.balance());
        tx.addEntry(user.id(), Transaction.Entry.Direction.CREDIT, amount, user.balance());
        tx.complete();
        tx.addTransfer(system.id(), user.id(), amount);

        saveAll(system, user, tx);
        return tx.id();
    }

    // user -> user, two-phase. Holds the funds on `from`; `to` is untouched until settle().
    @Transactional
    public TransactionId transfer(AccountId fromId, AccountId toId, Money amount) {
        Account from = loadAccount(fromId);
        Account to = loadAccount(toId); // receiver — also fails fast if it doesn't exist
        from.hold(amount);

        Transaction hold = Transaction.start(Transaction.Type.TRANSFER, partyOf(from), partyOf(to), amount,
                Transaction.Stage.HOLD, null);
        hold.addEntry(from.id(), Transaction.Entry.Direction.DEBIT, amount, from.balance());

        accountRepository.save(from);
        transactionRepository.save(hold);
        publishEvents(from);
        return hold.id();
    }

    // user -> system, two-phase. Holds the funds; the money reaches system only on settle (success).
    @Transactional
    public TransactionId cashout(AccountId userAccountId, Money amount) {
        Account userAccount = loadAccount(userAccountId);
        Account system = loadSystemAccount(userAccount.currency()); // receiver — also fails fast if the house account is missing
        userAccount.hold(amount);

        Transaction hold = Transaction.start(Transaction.Type.CASHOUT, partyOf(userAccount), partyOf(system), amount,
                Transaction.Stage.HOLD, null);
        hold.addEntry(userAccount.id(), Transaction.Entry.Direction.DEBIT, amount, userAccount.balance());

        accountRepository.save(userAccount);
        transactionRepository.save(hold);
        publishEvents(userAccount);
        return hold.id();
    }

    @Transactional
    public TransactionId settle(TransactionId txId) {
        Transaction hold = loadTransaction(txId);
        if (hold.stage() != Transaction.Stage.HOLD)
            throw new IllegalArgumentException("Cannot settle a " + hold.type() + " transaction with no pending hold");
        hold.complete(); // throws if already resolved — fail fast before touching accounts

        Account sender = resolveParty(hold.sender());
        Account receiver = resolveParty(hold.receiver());
        settleByType(hold.type(), sender, receiver, hold.amount());

        Transaction settlement = Transaction.start(hold.type(), hold.sender(), hold.receiver(), hold.amount(),
                Transaction.Stage.SETTLE, hold.id());
        settlement.addEntry(receiver.id(), Transaction.Entry.Direction.CREDIT, hold.amount(), receiver.balance());
        settlement.complete();
        settlement.addTransfer(sender.id(), receiver.id(), hold.amount());

        saveAll(sender, receiver, hold, settlement);
        return settlement.id();
    }


    private void settleByType(Transaction.Type type, Account sender, Account receiver, Money amount) {
        switch (type) {
            case CASHOUT, TRANSFER -> {
                sender.settle(amount);    // hold resolved successfully — clear it, money already left the balance
                receiver.deposit(amount); // ...and lands with the receiver (system for cashout, the other user for transfer)
            }
            default -> throw new IllegalArgumentException("No settle handling for " + type);
        }
    }

    @Transactional
    public void release(TransactionId txId) {
        Transaction hold = loadTransaction(txId);
        if (hold.stage() != Transaction.Stage.HOLD)
            throw new IllegalArgumentException("Cannot release a " + hold.type() + " transaction with no pending hold");
        hold.fail(); // throws if already resolved — fail fast before touching accounts

        Account holder = resolveParty(hold.sender());
        releaseByType(hold.type(), holder, hold.amount());

        Transaction releaseTx = Transaction.start(hold.type(), hold.sender(), hold.receiver(), hold.amount(),
                Transaction.Stage.RELEASE, hold.id());
        releaseTx.addEntry(holder.id(), Transaction.Entry.Direction.CREDIT, hold.amount(), holder.balance());
        releaseTx.complete();

        accountRepository.save(holder);
        transactionRepository.save(hold);
        transactionRepository.save(releaseTx);
        publishEvents(holder);
    }

    private void releaseByType(Transaction.Type type, Account holder, Money amount) {
        switch (type) {
            case CASHOUT, TRANSFER -> holder.release(amount); // undo the hold — only the sender was ever touched
            default -> throw new IllegalArgumentException("No release handling for " + type);
        }
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

    private void saveAll(Account a, Account b, Transaction... transactions) {
        accountRepository.save(a);
        accountRepository.save(b);
        for (Transaction tx : transactions) transactionRepository.save(tx);
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
