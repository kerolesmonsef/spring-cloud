package com.keroles.ewalletddd.topup.presentation.responses;

import com.keroles.ewalletddd.topup.domain.model.TopupRequest;
import com.keroles.ewalletddd.topup.domain.valueObject.LedgerTransactionRef;

import java.math.BigDecimal;

public record TopupResponse(String id, Long accountRef, BigDecimal amount, String currency,
                            String rail, String status, String railReference, String ledgerTransactionRef) {
    public static TopupResponse from(TopupRequest t) {
        LedgerTransactionRef ref = t.ledgerTransactionRef();
        return new TopupResponse(
                t.id().value().toString(),
                t.account().value(),
                t.amount().amount(),
                t.amount().currency().code(),
                t.rail().name(),
                t.status().name(),
                t.railReference(),
                ref == null ? null : ref.value().toString());
    }
}
