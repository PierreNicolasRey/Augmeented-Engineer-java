package com.exalt.it.belair.domain.order.exceptions;

/**
 * Exception thrown when attempting to acknowledge an order that is not in PENDING status.
 */
public class OrderCannotBeAcknowledgedException extends RuntimeException {
    public OrderCannotBeAcknowledgedException(String message) {
        super(message);
    }
}
