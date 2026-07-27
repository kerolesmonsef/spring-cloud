package com.keroles.ewalletddd.accounting.application;

import com.keroles.ewalletddd.accounting.domain.model.Account;
import com.keroles.ewalletddd.accounting.domain.model.Transaction;
import com.keroles.ewalletddd.accounting.domain.repository.AccountRepository;
import com.keroles.ewalletddd.accounting.domain.repository.TransactionRepository;
import com.keroles.ewalletddd.accounting.domain.valueObject.AccountReference;
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

    private AccountReference fundedAccount(String amount) {
        AccountReference ref = accountApplicationService.openAccount(null, AED);
        transactionApplicationService.topup(ref, Money.of(amount, "AED"));
        return ref;
    }


    private Money systemBalance() {
        return accountRepository.findByTypeAndCurrency(AccountType.SYSTEM, AED)
                .orElseThrow().balance();
    }

    @Test
    void savingLoadedAccountUpdatesInsteadOfInserting() {
        AccountReference accountRef = accountApplicationService.openAccount(null, AED);
        UserId user = accountApplicationService.getAccount(accountRef).userId();
        transactionApplicationService.topup(accountRef, Money.of("100.00", "AED"));
        transactionApplicationService.topup(accountRef, Money.of("50.00", "AED"));

        assertEquals(1, accountApplicationService.getUserAccounts(user).size());
        assertEquals(Money.of("150.00", "AED"), accountApplicationService.getAccount(accountRef).balance());
    }

    @Test
    void openAccountForUnknownUserIsRejected() {
        assertThrows(IllegalArgumentException.class,
                () -> accountApplicationService.openAccount(new UserId(999_999_999L), AED));
    }

    @Test
    void reserveThenSettle_holdGoesToZero() {
        AccountReference ref = fundedAccount("100.00");
        Money systemBefore = systemBalance();
        TransactionId txId = transactionApplicationService.cashout(ref, Money.of("40.00", "AED"));

        transactionApplicationService.settle(txId);

        Account account = accountApplicationService.getAccount(ref);
        assertEquals(Money.of("60.00", "AED"), account.balance());
        assertEquals(Money.zero(AED), account.holdBalance());
        assertEquals(systemBefore.add(Money.of("40.00", "AED")), systemBalance());

        assertEquals(0, transactionRepository.findById(txId).orElseThrow().transfers().size());
    }

    @Test
    void reserveThenRelease_moneyBackToMain() {
        AccountReference ref = fundedAccount("100.00");
        Money systemBefore = systemBalance();
        TransactionId txId = transactionApplicationService.cashout(ref, Money.of("40.00", "AED"));

        transactionApplicationService.release(txId);

        Account account = accountApplicationService.getAccount(ref);
        assertEquals(Money.of("100.00", "AED"), account.balance());
        assertEquals(Money.zero(AED), account.holdBalance());
        assertEquals(systemBefore, systemBalance());

        assertEquals(0, transactionRepository.findById(txId).orElseThrow().transfers().size());
    }

    @Test
    void duplicateSettleIsRejected_idempotencyGuard() {
        AccountReference ref = fundedAccount("100.00");
        TransactionId txId = transactionApplicationService.cashout(ref, Money.of("40.00", "AED"));
        transactionApplicationService.settle(txId);

        assertThrows(IllegalStateException.class, () -> transactionApplicationService.settle(txId));
        assertEquals(Money.zero(AED), accountApplicationService.getAccount(ref).holdBalance());
    }

    @Test
    void cannotReserveMoreThanBalance() {
        AccountReference ref = fundedAccount("10.00");
        assertThrows(InsufficientBalanceException.class,
                () -> transactionApplicationService.cashout(ref, Money.of("10.01", "AED")));
    }

    @Test
    void transferHoldsFromSenderAndLeavesReceiverUntouched() {
        AccountReference from = fundedAccount("100.00");
        AccountReference to = accountApplicationService.openAccount(null, AED);

        TransactionId txId = transactionApplicationService.transfer(from, to, Money.of("30.00", "AED"));

        assertEquals(Money.of("70.00", "AED"), accountApplicationService.getAccount(from).balance());
        assertEquals(Money.of("30.00", "AED"), accountApplicationService.getAccount(from).holdBalance());
        assertEquals(Money.zero(AED), accountApplicationService.getAccount(to).balance());
        assertEquals(0, transactionRepository.findById(txId).orElseThrow().transfers().size());
    }

    @Test
    void transferSettleMovesHeldMoneyToReceiver() {
        AccountReference from = fundedAccount("100.00");
        AccountReference to = accountApplicationService.openAccount(null, AED);
        TransactionId txId = transactionApplicationService.transfer(from, to, Money.of("30.00", "AED"));

        TransactionId settlementId = transactionApplicationService.settle(txId);

        assertEquals(Money.of("70.00", "AED"), accountApplicationService.getAccount(from).balance());
        assertEquals(Money.zero(AED), accountApplicationService.getAccount(from).holdBalance());
        assertEquals(Money.of("30.00", "AED"), accountApplicationService.getAccount(to).balance());

        assertEquals(0, transactionRepository.findById(txId).orElseThrow().transfers().size());
        var transfers = transactionRepository.findById(settlementId).orElseThrow().transfers();
        assertEquals(1, transfers.size());
        Transaction.Transfer transfer = transfers.get(0);
        assertEquals(accountApplicationService.getAccount(from).id(), transfer.senderId());
        assertEquals(accountApplicationService.getAccount(to).id(), transfer.receiverId());
        assertEquals(Money.of("30.00", "AED"), transfer.amount());
    }

    @Test
    void topupWritesATransferRowFromSystemToUser() {
        AccountReference ref = accountApplicationService.openAccount(null, AED);
        Money systemBefore = systemBalance();

        TransactionId txId = transactionApplicationService.topup(ref, Money.of("100.00", "AED"));

        var transfers = transactionRepository.findById(txId).orElseThrow().transfers();
        assertEquals(1, transfers.size());
        Transaction.Transfer transfer = transfers.get(0);
        assertEquals(accountApplicationService.getAccount(ref).id(), transfer.receiverId());
        assertEquals(Money.of("100.00", "AED"), transfer.amount());
        assertEquals(systemBalance(), systemBefore.subtract(Money.of("100.00", "AED")));
    }

    @Test
    void onlyOneAccountPerUserPerCurrency() {
        AccountReference first = accountApplicationService.openAccount(null, AED);
        UserId user = accountApplicationService.getAccount(first).userId();
        assertThrows(IllegalStateException.class, () -> accountApplicationService.openAccount(user, AED));
    }
}
