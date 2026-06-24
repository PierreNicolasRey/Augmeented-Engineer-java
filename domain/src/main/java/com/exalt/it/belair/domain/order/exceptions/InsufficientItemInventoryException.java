package com.exalt.it.belair.domain.order.exceptions;

public class InsufficientItemInventoryException extends RuntimeException {
    private final String itemId;
    private final int requestedQuantity;
    private final int availableQuantity;

    public InsufficientItemInventoryException(String message, String itemId, int requestedQuantity, int availableQuantity) {
        super(message);
        this.itemId = itemId;
        this.requestedQuantity = requestedQuantity;
        this.availableQuantity = availableQuantity;
    }

    public String getItemId() {
        return itemId;
    }

    public int getRequestedQuantity() {
        return requestedQuantity;
    }

    public int getAvailableQuantity() {
        return availableQuantity;
    }
}
