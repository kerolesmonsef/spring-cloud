package com.keroles.ewalletddd.cashout.presentation.requests;

import java.math.BigDecimal;


public record CreateCashoutRequest(Long accountId, BigDecimal amount, String currency, String rail) {}
