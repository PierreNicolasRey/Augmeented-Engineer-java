package com.exalt.it.belair.domain.order.exceptions;

/**
 * Exception thrown when attempting to cancel an order that is not in PENDING status.
 * Only pending orders can be cancelled; acknowledged, ready, or already cancelled orders cannot be cancelled.
 */
public class OrderCannotBeCancelledException extends RuntimeException {
    public OrderCannotBeCancelledException(String message) {
        super(message);
    }
}
