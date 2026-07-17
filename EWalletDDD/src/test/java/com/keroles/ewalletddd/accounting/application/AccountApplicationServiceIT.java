package com.keroles.ewalletddd.accounting.application;

import com.keroles.ewalletddd.accounting.domain.model.Account;
import com.keroles.ewalletddd.accounting.domain.model.AccountId;
import com.keroles.ewalletddd.accounting.domain.exception.InsufficientBalanceException;
import com.keroles.ewalletddd.accounting.domain.model.Transaction;
import com.keroles.ewalletddd.shared.domain.Money;
import com.keroles.ewalletddd.shared.domain.UserId;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.Currency;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Whole-slice test through the real front door: app service -> aggregate -> adapter -> H2.
 * Proves the Option B save UPDATEs instead of INSERTing a duplicate.
 */
@SpringBootTest
class AccountApplicationServiceIT {

    @Autowired AccountApplicationService service;

    private final Currency AED = Currency.getInstance("AED");

    private AccountId fundedAccount(String amount) {
        AccountId id = service.openAccount(null, AED); // null -> registers a new user too
        service.deposit(id, Money.of(amount, "AED"));
        return id;
    }

    @Test
    void savingLoadedAccountUpdatesInsteadOfInserting() {
        AccountId accountId = service.openAccount(null, AED);
        UserId user = service.getAccount(accountId).userId();
        service.deposit(accountId, Money.of("100.00", "AED"));
        service.deposit(accountId, Money.of("50.00", "AED"));

        assertEquals(1, service.getUserAccounts(user).size()); // still ONE row
        assertEquals(Money.of("150.00", "AED"), service.getAccount(accountId).balance());
    }

    @Test
    void openAccountForUnknownUserIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> service.openAccount(null, AED));
    }

    @Test
    void reserveThenSettle_holdGoesToZero() {
        AccountId id = fundedAccount("100.00");
        Transaction.TransactionId txId = service.reserve(id, Money.of("40.00", "AED"));

        service.settle(txId);

        Account account = service.getAccount(id);
        assertEquals(Money.of("60.00", "AED"), account.balance());
        assertEquals(Money.zero(AED), account.holdBalance());
    }

    @Test
    void reserveThenRelease_moneyBackToMain() {
        AccountId id = fundedAccount("100.00");
        Transaction.TransactionId txId = service.reserve(id, Money.of("40.00", "AED"));

        service.release(txId);

        Account account = service.getAccount(id);
        assertEquals(Money.of("100.00", "AED"), account.balance());
        assertEquals(Money.zero(AED), account.holdBalance());
    }

    @Test
    void duplicateSettleIsRejected_idempotencyGuard() {
        AccountId id = fundedAccount("100.00");
        Transaction.TransactionId txId = service.reserve(id, Money.of("40.00", "AED"));
        service.settle(txId);

        assertThrows(IllegalStateException.class, () -> service.settle(txId));
        assertEquals(Money.zero(AED), service.getAccount(id).holdBalance()); // settled once, not twice
    }

    @Test
    void cannotReserveMoreThanBalance() {
        AccountId id = fundedAccount("10.00");
        assertThrows(InsufficientBalanceException.class,
                () -> service.reserve(id, Money.of("10.01", "AED")));
    }

    @Test
    void onlyOneAccountPerUserPerCurrency() {
        AccountId first = service.openAccount(null, AED);
        UserId user = service.getAccount(first).userId();
        assertThrows(IllegalStateException.class, () -> service.openAccount(user, AED));
    }
}
