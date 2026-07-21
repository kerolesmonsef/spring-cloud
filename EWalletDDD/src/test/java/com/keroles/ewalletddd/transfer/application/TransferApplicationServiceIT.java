package com.keroles.ewalletddd.transfer.application;

import com.keroles.ewalletddd.accounting.application.AccountApplicationService;
import com.keroles.ewalletddd.accounting.application.TransactionApplicationService;
import com.keroles.ewalletddd.accounting.domain.exception.InsufficientBalanceException;
import com.keroles.ewalletddd.accounting.domain.model.Account;
import com.keroles.ewalletddd.accounting.domain.valueObject.AccountId;
import com.keroles.ewalletddd.shared.domain.Currency;
import com.keroles.ewalletddd.shared.domain.Money;
import com.keroles.ewalletddd.transfer.domain.valueObject.LedgerAccountRef;
import com.keroles.ewalletddd.transfer.domain.valueObject.TransferId;
import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest
@RequiredArgsConstructor
class TransferApplicationServiceIT {

    private final TransferApplicationService transferService;
    private final AccountApplicationService accountService;
    private final TransactionApplicationService transactionService;

    private final Currency AED = Currency.of("AED");

    private LedgerAccountRef fundedAccount(String amount) {
        AccountId id = accountService.openAccount(null, AED);
        transactionService.topup(id, Money.of(amount, "AED"));
        return new LedgerAccountRef(id.value());
    }

    private Account ledger(LedgerAccountRef ref) {
        return accountService.getAccount(new AccountId(ref.value()));
    }

    @Test
    void requestTransfer_movesFundsAndSettlesImmediately() {
        LedgerAccountRef from = fundedAccount("100.00");
        LedgerAccountRef to = fundedAccount("0.00");

        TransferId id = transferService.requestTransfer(from, to, Money.of("40.00", "AED"));

        Account sender = ledger(from);
        Account receiver = ledger(to);
        assertEquals(Money.of("60.00", "AED"), sender.balance());
        assertEquals(Money.zero(AED), sender.holdBalance());
        assertEquals(Money.of("40.00", "AED"), receiver.balance());

        var transfer = transferService.get(id);
        assertEquals(from, transfer.fromAccount());
        assertEquals(to, transfer.toAccount());
    }

    @Test
    void cannotTransferMoreThanBalance() {
        LedgerAccountRef from = fundedAccount("10.00");
        LedgerAccountRef to = fundedAccount("0.00");

        assertThrows(InsufficientBalanceException.class,
                () -> transferService.requestTransfer(from, to, Money.of("10.01", "AED")));
    }

    @Test
    void cannotTransferToSameAccount() {
        LedgerAccountRef from = fundedAccount("10.00");

        assertThrows(IllegalArgumentException.class,
                () -> transferService.requestTransfer(from, from, Money.of("5.00", "AED")));
    }
}
