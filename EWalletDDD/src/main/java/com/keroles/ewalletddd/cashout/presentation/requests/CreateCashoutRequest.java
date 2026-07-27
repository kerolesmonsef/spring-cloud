package com.keroles.ewalletddd.cashout.presentation.requests;

import java.math.BigDecimal;


public record CreateCashoutRequest(String accountReference, BigDecimal amount, String currency, String rail) {}
