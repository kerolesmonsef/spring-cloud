package com.keroles.ewalletddd.transfer.infrastructure.ledger;

import com.keroles.ewalletddd.accounting.application.TransactionApplicationService;
import com.keroles.ewalletddd.accounting.application.TransferLegDTO;
import com.keroles.ewalletddd.accounting.domain.valueObject.AccountReference;
import com.keroles.ewalletddd.accounting.domain.valueObject.TransactionId;
import com.keroles.ewalletddd.shared.domain.Money;
import com.keroles.ewalletddd.transfer.domain.port.LedgerTransferPort;
import com.keroles.ewalletddd.transfer.domain.valueObject.FeeLeg;
import com.keroles.ewalletddd.transfer.domain.valueObject.LedgerAccountReference;
import com.keroles.ewalletddd.transfer.domain.valueObject.LedgerHoldRef;
import com.keroles.ewalletddd.transfer.domain.valueObject.LedgerSettleRef;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class LedgerTransferAdapter implements LedgerTransferPort {

    private final TransactionApplicationService ledger;

    public LedgerTransferAdapter(TransactionApplicationService ledger) {
        this.ledger = ledger;
    }

    @Override
    public LedgerHoldRef hold(LedgerAccountReference fromAccount, LedgerAccountReference toAccount, Money amount) {
        TransactionId tx = ledger.transfer(new AccountReference(fromAccount.value()), new AccountReference(toAccount.value()), amount);
        return new LedgerHoldRef(tx.value());
    }

    @Override
    public LedgerSettleRef settle(LedgerHoldRef holdRef, Money principal, List<FeeLeg> feeLegs) {
        List<TransferLegDTO> legs = feeLegs.stream().map(this::toDto).toList();
        TransactionId settle = ledger.settle(new TransactionId(holdRef.value()), principal, legs);
        return new LedgerSettleRef(settle.value());
    }

    private TransferLegDTO toDto(FeeLeg leg) {
        return new TransferLegDTO(new AccountReference(leg.senderId().value()), new AccountReference(leg.receiverId().value()), leg.amount());
    }
}
