package com.keroles.ewalletddd.transfer.presentation.requests;

import java.math.BigDecimal;

public record CreateTransferRequest(Long fromAccountId, Long toAccountId, BigDecimal amount, String currency) {}
