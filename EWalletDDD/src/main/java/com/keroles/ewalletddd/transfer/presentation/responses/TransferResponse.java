package com.keroles.ewalletddd.transfer.presentation.responses;

import com.keroles.ewalletddd.transfer.domain.model.Transfer;

import java.math.BigDecimal;

public record TransferResponse(String id, Long fromAccountRef, Long toAccountRef, BigDecimal amount, String currency,
                               String holdRef, String settleRef) {
    public static TransferResponse from(Transfer t) {
        return new TransferResponse(
                t.id().value().toString(),
                t.fromAccount().value(),
                t.toAccount().value(),
                t.amount().amount(),
                t.amount().currency().code(),
                t.holdRef().value().toString(),
                t.settleRef().value().toString());
    }
}
