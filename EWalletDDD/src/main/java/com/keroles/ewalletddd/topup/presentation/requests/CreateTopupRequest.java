package com.keroles.ewalletddd.topup.presentation.requests;

import java.math.BigDecimal;

// named Create... to avoid clashing with the TopupRequest aggregate
public record CreateTopupRequest(Long accountId, BigDecimal amount, String currency, String rail) {}
