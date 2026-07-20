package com.keroles.ewalletddd.cashout.infrastructure.persistence.mapper;

import com.keroles.ewalletddd.cashout.domain.model.CashoutRequest;
import com.keroles.ewalletddd.cashout.domain.valueObject.CashoutId;
import com.keroles.ewalletddd.cashout.domain.valueObject.LedgerAccountRef;
import com.keroles.ewalletddd.cashout.domain.valueObject.LedgerReservationRef;
import com.keroles.ewalletddd.cashout.domain.valueObject.Rail;
import com.keroles.ewalletddd.cashout.infrastructure.persistence.entity.CashoutRequestJpaEntity;
import com.keroles.ewalletddd.shared.domain.Currency;
import com.keroles.ewalletddd.shared.domain.Money;

public final class CashoutRequestMapper {

    private CashoutRequestMapper() {}

    public static CashoutRequest toDomain(CashoutRequestJpaEntity row) {
        Currency currency = Currency.of(row.getCurrency());
        return CashoutRequest.restore(
                new CashoutId(row.getId()),
                new LedgerAccountRef(row.getAccountRef()),
                new Money(row.getAmount(), currency),
                Rail.valueOf(row.getRail()),
                new LedgerReservationRef(row.getLedgerReservationRef()),
                CashoutRequest.Status.valueOf(row.getStatus()),
                row.getRailReference(),
                row.getCreatedAt());
    }

    
    public static void copyOnto(CashoutRequest c, CashoutRequestJpaEntity row) {
        row.setId(c.id().value());
        row.setAccountRef(c.account().value());
        row.setAmount(c.amount().amount());
        row.setCurrency(c.amount().currency().code());
        row.setRail(c.rail().name());
        row.setStatus(c.status().name());
        row.setLedgerReservationRef(c.reservationRef().value());
        row.setRailReference(c.railReference());
        row.setLedgerSettleRef(c.settleReference() != null ? c.settleReference().value() : null);
        row.setCreatedAt(c.createdAt());
    }
}
