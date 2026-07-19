package com.keroles.ewalletddd.cashout.domain.exception;

// extends IllegalStateException so the scoped 409 handler maps it, while naming the domain rule
public class IllegalCashoutStateException extends IllegalStateException {
    public IllegalCashoutStateException(String message) { super(message); }
}
