package com.keroles.ddd.sharedkernel;

public class DomainException extends RuntimeException {

    public DomainException(String message) {
        super(message);
    }
}
