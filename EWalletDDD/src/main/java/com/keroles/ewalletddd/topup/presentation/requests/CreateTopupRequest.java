package com.keroles.ewalletddd.topup.presentation.requests;

import java.math.BigDecimal;


public record CreateTopupRequest(Long accountId, BigDecimal amount, String currency, String rail) {}
