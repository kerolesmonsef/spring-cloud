package com.keroles.ewalletddd.topup.infrastructure.persistence.mapper;

import com.keroles.ewalletddd.topup.domain.model.TopupRequest;
import com.keroles.ewalletddd.topup.domain.valueObject.LedgerAccountRef;
import com.keroles.ewalletddd.topup.domain.valueObject.LedgerTransactionRef;
import com.keroles.ewalletddd.topup.domain.valueObject.Rail;
import com.keroles.ewalletddd.topup.domain.valueObject.TopupId;
import com.keroles.ewalletddd.topup.infrastructure.persistence.entity.TopupRequestJpaEntity;
import com.keroles.ewalletddd.shared.domain.Currency;
import com.keroles.ewalletddd.shared.domain.Money;

public final class TopupRequestMapper {

    private TopupRequestMapper() {}

    public static TopupRequest toDomain(TopupRequestJpaEntity row) {
        Currency currency = Currency.of(row.getCurrency());
        return TopupRequest.restore(
                new TopupId(row.getId()),
                new LedgerAccountRef(row.getAccountRef()),
                new Money(row.getAmount(), currency),
                Rail.valueOf(row.getRail()),
                TopupRequest.Status.valueOf(row.getStatus()),
                row.getRailReference(),
                row.getLedgerTransactionRef() == null ? null : new LedgerTransactionRef(row.getLedgerTransactionRef()),
                row.getCreatedAt());
    }

    // copies onto a MANAGED entity so dirty-checking issues an UPDATE and @Version increments (Option B)
    public static void copyOnto(TopupRequest t, TopupRequestJpaEntity row) {
        row.setId(t.id().value());
        row.setAccountRef(t.account().value());
        row.setAmount(t.amount().amount());
        row.setCurrency(t.amount().currency().code());
        row.setRail(t.rail().name());
        row.setStatus(t.status().name());
        row.setRailReference(t.railReference());
        row.setLedgerTransactionRef(t.ledgerTransactionRef() == null ? null : t.ledgerTransactionRef().value());
        row.setCreatedAt(t.createdAt());
    }
}
