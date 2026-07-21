package com.keroles.ewalletddd.transfer.infrastructure.persistence.mapper;

import com.keroles.ewalletddd.transfer.domain.model.Transfer;
import com.keroles.ewalletddd.transfer.domain.valueObject.LedgerAccountRef;
import com.keroles.ewalletddd.transfer.domain.valueObject.LedgerHoldRef;
import com.keroles.ewalletddd.transfer.domain.valueObject.LedgerSettleRef;
import com.keroles.ewalletddd.transfer.domain.valueObject.TransferId;
import com.keroles.ewalletddd.transfer.infrastructure.persistence.entity.TransferRequestJpaEntity;
import com.keroles.ewalletddd.shared.domain.Currency;
import com.keroles.ewalletddd.shared.domain.Money;

public final class TransferMapper {

    private TransferMapper() {}

    public static Transfer toDomain(TransferRequestJpaEntity row) {
        Currency currency = Currency.of(row.getCurrency());
        return Transfer.restore(
                new TransferId(row.getId()),
                new LedgerAccountRef(row.getFromAccountRef()),
                new LedgerAccountRef(row.getToAccountRef()),
                new Money(row.getAmount(), currency),
                new LedgerHoldRef(row.getLedgerHoldRef()),
                new LedgerSettleRef(row.getLedgerSettleRef()),
                row.getCreatedAt());
    }

    public static void copyOnto(Transfer t, TransferRequestJpaEntity row) {
        row.setId(t.id().value());
        row.setFromAccountRef(t.fromAccount().value());
        row.setToAccountRef(t.toAccount().value());
        row.setAmount(t.amount().amount());
        row.setCurrency(t.amount().currency().code());
        row.setLedgerHoldRef(t.holdRef().value());
        row.setLedgerSettleRef(t.settleRef().value());
        row.setCreatedAt(t.createdAt());
    }
}
