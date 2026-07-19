package com.keroles.ewalletddd.cashout.infrastructure.persistence.mapper;

import com.keroles.ewalletddd.cashout.domain.model.CashoutRequest;
import com.keroles.ewalletddd.cashout.domain.valueObject.CashoutId;
import com.keroles.ewalletddd.cashout.domain.valueObject.LedgerAccountRef;
import com.keroles.ewalletddd.cashout.domain.valueObject.LedgerReservationRef;
import com.keroles.ewalletddd.cashout.domain.valueObject.Rail;
import com.keroles.ewalletddd.cashout.infrastructure.persistence.entity.CashoutRequestJpaEntity;
import com.keroles.ewalletddd.shared.domain.Money;

import java.util.Currency;

public final class CashoutRequestMapper {

    private CashoutRequestMapper() {}

    public static CashoutRequest toDomain(CashoutRequestJpaEntity row) {
        Currency currency = Currency.getInstance(row.getCurrency());
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

    // copies onto a MANAGED entity so dirty-checking issues an UPDATE and @Version increments (Option B)
    public static void copyOnto(CashoutRequest c, CashoutRequestJpaEntity row) {
        row.setId(c.id().value());
        row.setAccountRef(c.account().value());
        row.setAmount(c.amount().amount());
        row.setCurrency(c.amount().currency().getCurrencyCode());
        row.setRail(c.rail().name());
        row.setStatus(c.status().name());
        row.setLedgerReservationRef(c.reservationRef().value());
        row.setRailReference(c.railReference());
        row.setCreatedAt(c.createdAt());
    }
}
