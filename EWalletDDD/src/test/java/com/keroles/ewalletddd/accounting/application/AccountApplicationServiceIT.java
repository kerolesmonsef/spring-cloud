package com.keroles.ewalletddd.accounting.application;

import com.keroles.ewalletddd.accounting.domain.model.Account;
import com.keroles.ewalletddd.accounting.domain.valueObject.AccountId;
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

    private final Currency AED = Currency.of("AED");

    private AccountId fundedAccount(String amount) {
        AccountId id = accountApplicationService.openAccount(null, AED); // null -> registers a new user too
        transactionApplicationService.deposit(id, Money.of(amount, "AED"));
        return id;
    }

    @Test
    void savingLoadedAccountUpdatesInsteadOfInserting() {
        AccountId accountId = accountApplicationService.openAccount(null, AED);
        UserId user = accountApplicationService.getAccount(accountId).userId();
        transactionApplicationService.deposit(accountId, Money.of("100.00", "AED"));
        transactionApplicationService.deposit(accountId, Money.of("50.00", "AED"));

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
        TransactionId txId = transactionApplicationService.reserve(id, Money.of("40.00", "AED"));

        transactionApplicationService.settle(txId);

        Account account = accountApplicationService.getAccount(id);
        assertEquals(Money.of("60.00", "AED"), account.balance());
        assertEquals(Money.zero(AED), account.holdBalance());
    }

    @Test
    void reserveThenRelease_moneyBackToMain() {
        AccountId id = fundedAccount("100.00");
        TransactionId txId = transactionApplicationService.reserve(id, Money.of("40.00", "AED"));

        transactionApplicationService.release(txId);

        Account account = accountApplicationService.getAccount(id);
        assertEquals(Money.of("100.00", "AED"), account.balance());
        assertEquals(Money.zero(AED), account.holdBalance());
    }

    @Test
    void duplicateSettleIsRejected_idempotencyGuard() {
        AccountId id = fundedAccount("100.00");
        TransactionId txId = transactionApplicationService.reserve(id, Money.of("40.00", "AED"));
        transactionApplicationService.settle(txId);

        assertThrows(IllegalStateException.class, () -> transactionApplicationService.settle(txId));
        assertEquals(Money.zero(AED), accountApplicationService.getAccount(id).holdBalance()); // settled once, not twice
    }

    @Test
    void cannotReserveMoreThanBalance() {
        AccountId id = fundedAccount("10.00");
        assertThrows(InsufficientBalanceException.class,
                () -> transactionApplicationService.reserve(id, Money.of("10.01", "AED")));
    }

    @Test
    void onlyOneAccountPerUserPerCurrency() {
        AccountId first = accountApplicationService.openAccount(null, AED);
        UserId user = accountApplicationService.getAccount(first).userId();
        assertThrows(IllegalStateException.class, () -> accountApplicationService.openAccount(user, AED));
    }
}
