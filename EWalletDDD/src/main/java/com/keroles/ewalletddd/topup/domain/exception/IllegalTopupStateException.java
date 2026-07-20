package com.keroles.ewalletddd.topup.domain.exception;

// extends IllegalStateException so the scoped 409 handler maps it, while naming the domain rule
public class IllegalTopupStateException extends IllegalStateException {
    public IllegalTopupStateException(String message) { super(message); }
}
