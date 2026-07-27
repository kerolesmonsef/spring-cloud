package com.keroles.ewalletddd.transfer.presentation.requests;

import java.math.BigDecimal;

public record CreateTransferRequest(String fromAccountReference, String toAccountReference, BigDecimal amount, String currency) {}
