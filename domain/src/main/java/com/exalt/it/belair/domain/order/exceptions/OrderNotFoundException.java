package com.exalt.it.belair.domain.order.exceptions;

/**
 * Exception thrown when an order is not found in the repository.
 */
public class OrderNotFoundException extends RuntimeException {
    public OrderNotFoundException(String message) {
        super(message);
    }
}
