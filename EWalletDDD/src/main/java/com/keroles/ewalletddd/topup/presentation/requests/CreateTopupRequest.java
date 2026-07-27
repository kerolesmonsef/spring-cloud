package com.keroles.ewalletddd.topup.presentation.requests;

import java.math.BigDecimal;


public record CreateTopupRequest(String accountReference, BigDecimal amount, String currency, String rail) {}
