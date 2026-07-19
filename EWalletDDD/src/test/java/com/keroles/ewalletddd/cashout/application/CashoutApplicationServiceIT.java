package com.keroles.ewalletddd.cashout.application;

import com.keroles.ewalletddd.accounting.application.AccountApplicationService;
import com.keroles.ewalletddd.accounting.application.TransactionApplicationService;
import com.keroles.ewalletddd.accounting.domain.model.Account;
import com.keroles.ewalletddd.accounting.domain.exception.InsufficientBalanceException;
import com.keroles.ewalletddd.accounting.domain.valueObject.AccountId;
import com.keroles.ewalletddd.cashout.domain.model.CashoutRequest;
import com.keroles.ewalletddd.cashout.domain.valueObject.CashoutId;
import com.keroles.ewalletddd.cashout.domain.valueObject.LedgerAccountRef;
import com.keroles.ewalletddd.cashout.domain.valueObject.Rail;
import com.keroles.ewalletddd.shared.domain.Money;
import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import com.keroles.ewalletddd.shared.domain.Currency;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

// drives Cashout through the REAL Accounting front door (via the ACL) + fake rails, over H2
@SpringBootTest
@RequiredArgsConstructor
class CashoutApplicationServiceIT {

    private final CashoutApplicationService cashoutService;
    private final AccountApplicationService accountService;
    private final TransactionApplicationService transactionService;

    private final Currency AED = Currency.of("AED");

    private LedgerAccountRef fundedAccount(String amount) {
        AccountId id = accountService.openAccount(null, AED); // null -> registers a new user too
        transactionService.deposit(id, Money.of(amount, "AED"));
        return new LedgerAccountRef(id.value());
    }

    private Account ledger(LedgerAccountRef ref) {
        return accountService.getAccount(new AccountId(ref.value()));
    }

    @Test
    void requestPlacesHold_thenConfirmSettles() {
        LedgerAccountRef acc = fundedAccount("100.00");

        CashoutId id = cashoutService.requestCashout(acc, Money.of("40.00", "AED"), Rail.AANI);

        Account held = ledger(acc);
        assertEquals(Money.of("60.00", "AED"), held.balance());      // main reduced by the hold
        assertEquals(Money.of("40.00", "AED"), held.holdBalance());  // held
        assertEquals(CashoutRequest.Status.DISPATCHED, cashoutService.get(id).status());

        cashoutService.confirm(id);

        Account settled = ledger(acc);
        assertEquals(Money.of("60.00", "AED"), settled.balance());
        assertEquals(Money.zero(AED), settled.holdBalance());        // hold cleared
        assertEquals(CashoutRequest.Status.CONFIRMED, cashoutService.get(id).status());
    }

    @Test
    void failReleasesHoldBackToMain() {
        LedgerAccountRef acc = fundedAccount("100.00");

        CashoutId id = cashoutService.requestCashout(acc, Money.of("40.00", "AED"), Rail.LULU);
        cashoutService.fail(id);

        Account released = ledger(acc);
        assertEquals(Money.of("100.00", "AED"), released.balance()); // back to main
        assertEquals(Money.zero(AED), released.holdBalance());
        assertEquals(CashoutRequest.Status.FAILED, cashoutService.get(id).status());
    }

    @Test
    void doubleConfirmIsRejected_holdSettledOnce() {
        LedgerAccountRef acc = fundedAccount("100.00");
        CashoutId id = cashoutService.requestCashout(acc, Money.of("30.00", "AED"), Rail.LULU); // async rail
        cashoutService.confirm(id);

        assertThrows(IllegalStateException.class, () -> cashoutService.confirm(id));
        assertEquals(Money.zero(AED), ledger(acc).holdBalance());    // settled once, not twice
    }

    @Test
    void syncRailConfirmsAtDispatch_noCallbackNeeded() {
        LedgerAccountRef acc = fundedAccount("100.00");

        CashoutId id = cashoutService.requestCashout(acc, Money.of("40.00", "AED"), Rail.MBANK);

        // Mbank is synchronous — settled inside requestCashout, no confirm() call
        Account after = ledger(acc);
        assertEquals(Money.of("60.00", "AED"), after.balance());
        assertEquals(Money.zero(AED), after.holdBalance());          // hold already cleared
        assertEquals(CashoutRequest.Status.CONFIRMED, cashoutService.get(id).status());

        // a late callback on an already-final cashout is rejected
        assertThrows(IllegalStateException.class, () -> cashoutService.confirm(id));
    }

    @Test
    void cannotCashoutMoreThanBalance() {
        LedgerAccountRef acc = fundedAccount("10.00");
        assertThrows(InsufficientBalanceException.class,
                () -> cashoutService.requestCashout(acc, Money.of("10.01", "AED"), Rail.AANI));
    }
}
