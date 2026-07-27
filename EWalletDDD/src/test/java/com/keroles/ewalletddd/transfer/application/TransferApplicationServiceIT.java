package com.keroles.ewalletddd.transfer.application;

import com.keroles.ewalletddd.accounting.application.AccountApplicationService;
import com.keroles.ewalletddd.accounting.application.TransactionApplicationService;
import com.keroles.ewalletddd.accounting.domain.exception.InsufficientBalanceException;
import com.keroles.ewalletddd.accounting.domain.model.Account;
import com.keroles.ewalletddd.accounting.domain.model.Transaction;
import com.keroles.ewalletddd.accounting.domain.repository.AccountRepository;
import com.keroles.ewalletddd.accounting.domain.repository.TransactionRepository;
import com.keroles.ewalletddd.accounting.domain.valueObject.AccountId;
import com.keroles.ewalletddd.accounting.domain.valueObject.AccountReference;
import com.keroles.ewalletddd.accounting.domain.valueObject.TransactionId;
import com.keroles.ewalletddd.shared.domain.Currency;
import com.keroles.ewalletddd.shared.domain.Money;
import com.keroles.ewalletddd.transfer.domain.valueObject.LedgerAccountReference;
import com.keroles.ewalletddd.transfer.domain.valueObject.TransferId;
import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@RequiredArgsConstructor
class TransferApplicationServiceIT {

    private final TransferApplicationService transferService;
    private final AccountApplicationService accountService;
    private final TransactionApplicationService transactionService;
    private final TransactionRepository transactionRepository;
    private final AccountRepository accountRepository;

    private final Currency AED = Currency.of("AED");

    private LedgerAccountReference fundedAccount(String amount) {
        AccountReference ref = accountService.openAccount(null, AED);
        transactionService.topup(ref, Money.of(amount, "AED"));
        return new LedgerAccountReference(ref.value());
    }

    private Account ledger(LedgerAccountReference ref) {
        return accountService.getAccount(new AccountReference(ref.value()));
    }

    @Test
    void requestTransfer_movesFundsAndSettlesImmediately() {
        LedgerAccountReference from = fundedAccount("100.00");
        LedgerAccountReference to = fundedAccount("0.00");
        Money feeBefore = accountService.getAccount(AccountReference.FEE).balance();
        Money vatBefore = accountService.getAccount(AccountReference.VAT).balance();

        TransferId id = transferService.requestTransfer(from, to, Money.of("40.00", "AED"));

        Account sender = ledger(from);
        Account receiver = ledger(to);
        assertEquals(Money.of("59.58", "AED"), sender.balance());
        assertEquals(Money.zero(AED), sender.holdBalance());
        assertEquals(Money.of("40.00", "AED"), receiver.balance());

        Money feeAfter = accountService.getAccount(AccountReference.FEE).balance();
        Money vatAfter = accountService.getAccount(AccountReference.VAT).balance();
        assertEquals(Money.of("0.40", "AED"), feeAfter.subtract(feeBefore));
        assertEquals(Money.of("0.02", "AED"), vatAfter.subtract(vatBefore));

        var transfer = transferService.get(id);
        assertEquals(from, transfer.fromAccount());
        assertEquals(to, transfer.toAccount());
    }

    @Test
    void settlementRecordsFeeAndVatLegsButSkipsZeroAmounts() {
        LedgerAccountReference from = fundedAccount("100.00");
        LedgerAccountReference to = fundedAccount("0.00");

        TransferId id = transferService.requestTransfer(from, to, Money.of("40.00", "AED"));

        var transfer = transferService.get(id);
        Transaction settlement = transactionRepository.findById(new TransactionId(transfer.settleRef().value()))
                .orElseThrow();
        var legs = settlement.transfers();

        AccountId fromId = ledger(from).id();
        AccountId toId = ledger(to).id();
        AccountId feeAccountId = accountRepository.findByReference(AccountReference.FEE).orElseThrow().id();
        AccountId vatAccountId = accountRepository.findByReference(AccountReference.VAT).orElseThrow().id();

        assertEquals(3, legs.size());
        assertTrue(legs.stream().anyMatch(t -> t.amount().equals(Money.of("40.00", "AED"))
                && t.senderId().equals(fromId)
                && t.receiverId().equals(toId)));
        assertTrue(legs.stream().anyMatch(t -> t.amount().equals(Money.of("0.40", "AED"))
                && t.senderId().equals(fromId)
                && t.receiverId().equals(feeAccountId)));
        assertTrue(legs.stream().anyMatch(t -> t.amount().equals(Money.of("0.02", "AED"))
                && t.senderId().equals(fromId)
                && t.receiverId().equals(vatAccountId)));
    }

    @Test
    void cannotTransferMoreThanBalance() {
        LedgerAccountReference from = fundedAccount("10.00");
        LedgerAccountReference to = fundedAccount("0.00");

        assertThrows(InsufficientBalanceException.class,
                () -> transferService.requestTransfer(from, to, Money.of("10.01", "AED")));
    }

    @Test
    void cannotTransferToSameAccount() {
        LedgerAccountReference from = fundedAccount("10.00");

        assertThrows(IllegalArgumentException.class,
                () -> transferService.requestTransfer(from, from, Money.of("5.00", "AED")));
    }
}
