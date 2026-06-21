package com.exalt.it.belair.domain.order.exceptions;

public class InvalidItemTypeException extends RuntimeException {
    public InvalidItemTypeException(String message) {
        super(message);
    }
}
