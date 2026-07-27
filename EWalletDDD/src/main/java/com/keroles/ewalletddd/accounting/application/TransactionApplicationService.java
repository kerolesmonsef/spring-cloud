package com.keroles.ewalletddd.accounting.application;

import com.keroles.ewalletddd.accounting.domain.model.Account;
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

import java.util.List;

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
    public TransactionId topup(AccountReference userAccountRef, Money amount) {
        Account user = loadAccount(userAccountRef);
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

    
    @Transactional
    public TransactionId transfer(AccountReference fromRef, AccountReference toRef, Money amount) {
        Account fromAccount = loadAccount(fromRef);
        Account toAccount = loadAccount(toRef);
        fromAccount.hold(amount);

        Transaction holdTransaction = Transaction.start(Transaction.Type.TRANSFER, partyOf(fromAccount), partyOf(toAccount), amount,
                Transaction.Stage.HOLD, null);
        holdTransaction.addEntry(fromAccount.id(), Transaction.Entry.Direction.DEBIT, amount, fromAccount.balance());

        accountRepository.save(fromAccount);
        transactionRepository.save(holdTransaction);
        publishEvents(fromAccount);
        return holdTransaction.id();
    }

    
    @Transactional
    public TransactionId cashout(AccountReference userAccountRef, Money amount) {
        Account userAccount = loadAccount(userAccountRef);
        Account system = loadSystemAccount(userAccount.currency()); 
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
        return settle(txId, List.of());
    }

    @Transactional
    public TransactionId settle(TransactionId txId, List<TransferLegDTO> feeLegs) {
        Transaction holdTransaction = loadTransaction(txId);
        return settle(txId, holdTransaction.amount(), feeLegs);
    }

    @Transactional
    public TransactionId settle(TransactionId txId, Money receiverCredit, List<TransferLegDTO> feeLegs) {
        Transaction holdTransaction = loadTransaction(txId);
        if (holdTransaction.stage() != Transaction.Stage.HOLD)
            throw new IllegalArgumentException("Cannot settle a " + holdTransaction.type() + " transaction with no pending hold");
        holdTransaction.complete();

        Account sender = resolveParty(holdTransaction.sender());
        Account receiver = resolveParty(holdTransaction.receiver());
        settleByType(holdTransaction.type(), sender, receiver, holdTransaction.amount(), receiverCredit);

        Transaction settlementTransaction = Transaction.start(holdTransaction.type(), holdTransaction.sender(), holdTransaction.receiver(), holdTransaction.amount(),
                Transaction.Stage.SETTLE, holdTransaction.id());
        settlementTransaction.addEntry(receiver.id(), Transaction.Entry.Direction.CREDIT, receiverCredit, receiver.balance());
        settlementTransaction.complete();

        settlementTransaction.addTransfer(sender.id(), receiver.id(), receiverCredit);
        for (TransferLegDTO leg : feeLegs) {
            if (leg.amount().isZero()) continue;
            Account legSender = loadAccount(leg.senderId());
            Account legReceiver = loadAccount(leg.receiverId());
            if (leg.senderId().equals(sender.reference())) {
                legReceiver.deposit(leg.amount());
                accountRepository.save(legReceiver);
                publishEvents(legReceiver);
            }
            settlementTransaction.addTransfer(legSender.id(), legReceiver.id(), leg.amount());
        }

        saveAll(sender, receiver, holdTransaction, settlementTransaction);
        return settlementTransaction.id();
    }

    private void settleByType(Transaction.Type type, Account sender, Account receiver, Money heldAmount, Money receiverCredit) {
        switch (type) {
            case CASHOUT, TRANSFER -> {
                sender.settle(heldAmount);
                receiver.deposit(receiverCredit);
            }
            default -> throw new IllegalArgumentException("No settle handling for " + type);
        }
    }

    @Transactional
    public void release(TransactionId txId) {
        Transaction hold = loadTransaction(txId);
        if (hold.stage() != Transaction.Stage.HOLD)
            throw new IllegalArgumentException("Cannot release a " + hold.type() + " transaction with no pending hold");
        hold.fail(); 

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
            case CASHOUT, TRANSFER -> holder.release(amount); 
            default -> throw new IllegalArgumentException("No release handling for " + type);
        }
    }

    
    
    private Account resolveParty(Party party) {
        return loadAccount(new AccountReference(party.reference()));
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


    private Account loadAccount(AccountReference reference) {
        return accountRepository.findByReference(reference)
                .orElseThrow(() -> new IllegalArgumentException("No account " + reference.value()));
    }

    private Transaction loadTransaction(TransactionId id) {
        return transactionRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("No transaction " + id.value()));
    }

    private void publishEvents(Account account) {
        account.pullEvents().forEach(eventPublisher::publishEvent);
    }
}
