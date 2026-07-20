package com.keroles.ewalletddd.accounting.domain.model;

import com.keroles.ewalletddd.accounting.domain.valueObject.AccountId;
import com.keroles.ewalletddd.accounting.domain.valueObject.AccountReference;
import com.keroles.ewalletddd.accounting.domain.valueObject.AccountType;
import com.keroles.ewalletddd.accounting.domain.valueObject.Party;
import com.keroles.ewalletddd.shared.domain.Money;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class TransactionTest {

    private final Party self = Party.internal(AccountReference.newRef(), AccountType.USER);
    private final Money amount = Money.of("40.00", "AED");
    private final Money balanceAfter = Money.of("60.00", "AED");

    @Test
    void withdrawalGoesFromUsToExternal() {
        Transaction tx = Transaction.withdrawal(new AccountId(1L), self, amount, balanceAfter);
        assertEquals(self, tx.sender());
        assertEquals(Party.EXTERNAL, tx.receiver());
        assertEquals(AccountType.EXTERNAL, tx.receiver().type());
    }

    @Test
    void depositComesFromExternalToUs() {
        Transaction tx = Transaction.deposit(new AccountId(1L), self, amount, balanceAfter);
        assertEquals(Party.EXTERNAL, tx.sender());
        assertEquals(self, tx.receiver());
    }

    @Test
    void holdSelfCorrelatesWhenNoParentGiven() {
        Transaction hold = Transaction.start(Transaction.Type.CASHOUT, self, self, amount, Transaction.Stage.HOLD, null);
        assertEquals(hold.id(), hold.parentCorrelationId());
    }

    @Test
    void settleChildSharesTheHoldsCorrelationId() {
        Transaction hold = Transaction.start(Transaction.Type.CASHOUT, self, self, amount, Transaction.Stage.HOLD, null);
        Transaction settlement = Transaction.start(hold.type(), hold.sender(), hold.receiver(), hold.amount(),
                Transaction.Stage.SETTLE, hold.id());

        assertEquals(hold.parentCorrelationId(), settlement.parentCorrelationId());
        assertNotEquals(hold.id(), settlement.id());
    }

    @Test
    void addTransferRejectedUnlessCompleted() {
        Transaction hold = Transaction.start(Transaction.Type.TRANSFER, self, self, amount, Transaction.Stage.HOLD, null);
        assertThrows(IllegalStateException.class,
                () -> hold.addTransfer(new AccountId(1L), new AccountId(2L), amount));

        hold.fail();
        assertThrows(IllegalStateException.class,
                () -> hold.addTransfer(new AccountId(1L), new AccountId(2L), amount));
    }

    @Test
    void addTransferRecordsSenderReceiverAmountWhenCompleted() {
        Transaction tx = Transaction.start(Transaction.Type.TOPUP, self, self, amount);
        tx.complete();
        tx.addTransfer(new AccountId(1L), new AccountId(2L), amount);

        assertEquals(1, tx.transfers().size());
        Transaction.Transfer transfer = tx.transfers().get(0);
        assertEquals(new AccountId(1L), transfer.senderId());
        assertEquals(new AccountId(2L), transfer.receiverId());
        assertEquals(amount, transfer.amount());
    }
}
