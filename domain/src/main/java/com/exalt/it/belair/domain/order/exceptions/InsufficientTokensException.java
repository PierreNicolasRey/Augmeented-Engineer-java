package com.exalt.it.belair.domain.order.exceptions;

public class InsufficientTokensException extends RuntimeException {
    public InsufficientTokensException(String message) {
        super(message);
    }
}
