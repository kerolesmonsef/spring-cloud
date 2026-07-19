package com.keroles.ewalletddd.cashout.presentation.requests;

import java.math.BigDecimal;

// named Create... to avoid clashing with the CashoutRequest aggregate
public record CreateCashoutRequest(Long accountId, BigDecimal amount, String currency, String rail) {}
