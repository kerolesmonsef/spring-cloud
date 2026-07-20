package com.keroles.ewalletddd.cashout.domain.exception;


public class IllegalCashoutStateException extends IllegalStateException {
    public IllegalCashoutStateException(String message) { super(message); }
}
