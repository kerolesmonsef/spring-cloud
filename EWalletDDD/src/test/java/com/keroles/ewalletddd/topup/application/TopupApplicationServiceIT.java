package com.keroles.ewalletddd.topup.application;

import com.keroles.ewalletddd.accounting.application.AccountApplicationService;
import com.keroles.ewalletddd.accounting.domain.model.Account;
import com.keroles.ewalletddd.accounting.domain.valueObject.AccountId;
import com.keroles.ewalletddd.topup.domain.model.TopupRequest;
import com.keroles.ewalletddd.topup.domain.valueObject.LedgerAccountRef;
import com.keroles.ewalletddd.topup.domain.valueObject.Rail;
import com.keroles.ewalletddd.topup.domain.valueObject.TopupId;
import com.keroles.ewalletddd.shared.domain.Currency;
import com.keroles.ewalletddd.shared.domain.Money;
import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;



@SpringBootTest
@RequiredArgsConstructor
class TopupApplicationServiceIT {

    private final TopupApplicationService topupService;
    private final AccountApplicationService accountService;

    private final Currency AED = Currency.of("AED");

    private LedgerAccountRef newAccount() {
        AccountId id = accountService.openAccount(null, AED); 
        return new LedgerAccountRef(id.value());
    }

    private Account ledger(LedgerAccountRef ref) {
        return accountService.getAccount(new AccountId(ref.value()));
    }

    @Test
    void syncRailCreditsAtDispatch_noCallbackNeeded() {
        LedgerAccountRef acc = newAccount();
        assertEquals(Money.zero(AED), ledger(acc).balance());

        TopupId id = topupService.requestTopup(acc, Money.of("40.00", "AED"), Rail.MBANK);

        
        assertEquals(Money.of("40.00", "AED"), ledger(acc).balance());
        TopupRequest done = topupService.get(id);
        assertEquals(TopupRequest.Status.COMPLETED, done.status());
        assertNotNull(done.ledgerTransactionRef(), "Ledger transaction linked on success");

        
        assertThrows(IllegalStateException.class, () -> topupService.confirm(id));
    }

    @Test
    void asyncRailStaysPending_untilConfirmCredits() {
        LedgerAccountRef acc = newAccount();

        TopupId id = topupService.requestTopup(acc, Money.of("40.00", "AED"), Rail.TCS);

        TopupRequest pending = topupService.get(id);
        assertEquals(TopupRequest.Status.PENDING, pending.status());
        assertEquals(Money.zero(AED), ledger(acc).balance(), "not credited until the rail confirms");
        assertNull(pending.ledgerTransactionRef());

        topupService.confirm(id);

        assertEquals(Money.of("40.00", "AED"), ledger(acc).balance());
        assertEquals(TopupRequest.Status.COMPLETED, topupService.get(id).status());
    }

    @Test
    void asyncRailFail_leavesBalanceUntouched() {
        LedgerAccountRef acc = newAccount();

        TopupId id = topupService.requestTopup(acc, Money.of("40.00", "AED"), Rail.TCS);
        topupService.fail(id);

        assertEquals(TopupRequest.Status.FAILED, topupService.get(id).status());
        assertEquals(Money.zero(AED), ledger(acc).balance()); 
    }

    @Test
    void doubleConfirmIsRejected_creditedOnce() {
        LedgerAccountRef acc = newAccount();
        TopupId id = topupService.requestTopup(acc, Money.of("30.00", "AED"), Rail.TCS);
        topupService.confirm(id);

        
        assertThrows(IllegalStateException.class, () -> topupService.confirm(id));
        assertEquals(Money.of("30.00", "AED"), ledger(acc).balance()); 
    }
}
