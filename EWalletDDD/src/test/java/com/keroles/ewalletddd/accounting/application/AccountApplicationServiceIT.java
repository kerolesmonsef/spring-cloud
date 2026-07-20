package com.keroles.ewalletddd.accounting.application;

import com.keroles.ewalletddd.accounting.domain.model.Account;
import com.keroles.ewalletddd.accounting.domain.model.Transaction;
import com.keroles.ewalletddd.accounting.domain.repository.AccountRepository;
import com.keroles.ewalletddd.accounting.domain.repository.TransactionRepository;
import com.keroles.ewalletddd.accounting.domain.valueObject.AccountId;
import com.keroles.ewalletddd.accounting.domain.valueObject.AccountType;
import com.keroles.ewalletddd.accounting.domain.exception.InsufficientBalanceException;
import com.keroles.ewalletddd.accounting.domain.valueObject.TransactionId;
import com.keroles.ewalletddd.shared.domain.Money;
import com.keroles.ewalletddd.shared.domain.UserId;
import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import com.keroles.ewalletddd.shared.domain.Currency;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@RequiredArgsConstructor
class AccountApplicationServiceIT {

    private final AccountApplicationService accountApplicationService;
    private final TransactionApplicationService transactionApplicationService;
    private final AccountRepository accountRepository;
    private final TransactionRepository transactionRepository;

    private final Currency AED = Currency.of("AED");

    private AccountId fundedAccount(String amount) {
        AccountId id = accountApplicationService.openAccount(null, AED); // null -> registers a new user too
        transactionApplicationService.topup(id, Money.of(amount, "AED"));
        return id;
    }

    // system account is a shared, never-reset seed row — assert the delta settle() adds, not an absolute value
    private Money systemBalance() {
        return accountRepository.findByTypeAndCurrency(AccountType.SYSTEM, AED)
                .orElseThrow().balance();
    }

    @Test
    void savingLoadedAccountUpdatesInsteadOfInserting() {
        AccountId accountId = accountApplicationService.openAccount(null, AED);
        UserId user = accountApplicationService.getAccount(accountId).userId();
        transactionApplicationService.topup(accountId, Money.of("100.00", "AED"));
        transactionApplicationService.topup(accountId, Money.of("50.00", "AED"));

        assertEquals(1, accountApplicationService.getUserAccounts(user).size()); // still ONE row
        assertEquals(Money.of("150.00", "AED"), accountApplicationService.getAccount(accountId).balance());
    }

    @Test
    void openAccountForUnknownUserIsRejected() {
        assertThrows(IllegalArgumentException.class,
                () -> accountApplicationService.openAccount(new UserId(999_999_999L), AED));
    }

    @Test
    void reserveThenSettle_holdGoesToZero() {
        AccountId id = fundedAccount("100.00");
        Money systemBefore = systemBalance();
        TransactionId txId = transactionApplicationService.cashout(id, Money.of("40.00", "AED"));

        transactionApplicationService.settle(txId);

        Account account = accountApplicationService.getAccount(id);
        assertEquals(Money.of("60.00", "AED"), account.balance());
        assertEquals(Money.zero(AED), account.holdBalance());
        assertEquals(systemBefore.add(Money.of("40.00", "AED")), systemBalance()); // settled cashout lands with system

        assertEquals(0, transactionRepository.findById(txId).orElseThrow().transfers().size()); // hold row itself never gets one
    }

    @Test
    void reserveThenRelease_moneyBackToMain() {
        AccountId id = fundedAccount("100.00");
        Money systemBefore = systemBalance();
        TransactionId txId = transactionApplicationService.cashout(id, Money.of("40.00", "AED"));

        transactionApplicationService.release(txId);

        Account account = accountApplicationService.getAccount(id);
        assertEquals(Money.of("100.00", "AED"), account.balance());
        assertEquals(Money.zero(AED), account.holdBalance());
        assertEquals(systemBefore, systemBalance()); // released cashout never touches the receiver

        assertEquals(0, transactionRepository.findById(txId).orElseThrow().transfers().size()); // release is not a success
    }

    @Test
    void duplicateSettleIsRejected_idempotencyGuard() {
        AccountId id = fundedAccount("100.00");
        TransactionId txId = transactionApplicationService.cashout(id, Money.of("40.00", "AED"));
        transactionApplicationService.settle(txId);

        assertThrows(IllegalStateException.class, () -> transactionApplicationService.settle(txId));
        assertEquals(Money.zero(AED), accountApplicationService.getAccount(id).holdBalance()); // settled once, not twice
    }

    @Test
    void cannotReserveMoreThanBalance() {
        AccountId id = fundedAccount("10.00");
        assertThrows(InsufficientBalanceException.class,
                () -> transactionApplicationService.cashout(id, Money.of("10.01", "AED")));
    }

    @Test
    void transferHoldsFromSenderAndLeavesReceiverUntouched() {
        AccountId from = fundedAccount("100.00");
        AccountId to = accountApplicationService.openAccount(null, AED);

        TransactionId txId = transactionApplicationService.transfer(from, to, Money.of("30.00", "AED"));

        assertEquals(Money.of("70.00", "AED"), accountApplicationService.getAccount(from).balance());
        assertEquals(Money.of("30.00", "AED"), accountApplicationService.getAccount(from).holdBalance());
        assertEquals(Money.zero(AED), accountApplicationService.getAccount(to).balance());
        assertEquals(0, transactionRepository.findById(txId).orElseThrow().transfers().size()); // still pending, no transfer row yet
    }

    @Test
    void transferSettleMovesHeldMoneyToReceiver() {
        AccountId from = fundedAccount("100.00");
        AccountId to = accountApplicationService.openAccount(null, AED);
        TransactionId txId = transactionApplicationService.transfer(from, to, Money.of("30.00", "AED"));

        TransactionId settlementId = transactionApplicationService.settle(txId);

        assertEquals(Money.of("70.00", "AED"), accountApplicationService.getAccount(from).balance());
        assertEquals(Money.zero(AED), accountApplicationService.getAccount(from).holdBalance());
        assertEquals(Money.of("30.00", "AED"), accountApplicationService.getAccount(to).balance());

        assertEquals(0, transactionRepository.findById(txId).orElseThrow().transfers().size()); // hold row itself never gets one
        var transfers = transactionRepository.findById(settlementId).orElseThrow().transfers();
        assertEquals(1, transfers.size());
        Transaction.Transfer transfer = transfers.get(0);
        assertEquals(from, transfer.senderId());
        assertEquals(to, transfer.receiverId());
        assertEquals(Money.of("30.00", "AED"), transfer.amount());
    }

    @Test
    void topupWritesATransferRowFromSystemToUser() {
        AccountId id = accountApplicationService.openAccount(null, AED);
        Money systemBefore = systemBalance();

        TransactionId txId = transactionApplicationService.topup(id, Money.of("100.00", "AED"));

        var transfers = transactionRepository.findById(txId).orElseThrow().transfers();
        assertEquals(1, transfers.size());
        Transaction.Transfer transfer = transfers.get(0);
        assertEquals(id, transfer.receiverId());
        assertEquals(Money.of("100.00", "AED"), transfer.amount());
        assertEquals(systemBalance(), systemBefore.subtract(Money.of("100.00", "AED")));
    }

    @Test
    void onlyOneAccountPerUserPerCurrency() {
        AccountId first = accountApplicationService.openAccount(null, AED);
        UserId user = accountApplicationService.getAccount(first).userId();
        assertThrows(IllegalStateException.class, () -> accountApplicationService.openAccount(user, AED));
    }
}
