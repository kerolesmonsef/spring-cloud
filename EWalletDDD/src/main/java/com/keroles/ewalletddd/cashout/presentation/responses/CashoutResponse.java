package com.keroles.ewalletddd.cashout.presentation.responses;

import com.keroles.ewalletddd.cashout.domain.model.CashoutRequest;

import java.math.BigDecimal;

public record CashoutResponse(String id, Long accountRef, BigDecimal amount, String currency,
                              String rail, String status, String reservationRef, String railReference) {
    public static CashoutResponse from(CashoutRequest c) {
        return new CashoutResponse(
                c.id().value().toString(),
                c.account().value(),
                c.amount().amount(),
                c.amount().currency().getCurrencyCode(),
                c.rail().name(),
                c.status().name(),
                c.reservationRef().value().toString(),
                c.railReference());
    }
}
