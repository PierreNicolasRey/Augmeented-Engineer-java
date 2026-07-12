package com.exalt.it.belair.domain.order.model;

import java.util.List;

/**
 * Represents a request to change items in an existing order.
 * Contains the list of items to remove and items to add.
 */
public class OrderChangeRequest {
    private final String orderId;
    private final String requestedBy;
    private final List<OrderItem> itemsToRemove;
    private final List<OrderItem> itemsToAdd;
    
    /**
     * Constructs an OrderChangeRequest.
     * 
     * @param orderId the ID of the order to change
     * @param requestedBy the festival goer ID requesting the change
     * @param itemsToRemove list of items to remove from the order
     * @param itemsToAdd list of items to add to the order
     */
    public OrderChangeRequest(String orderId, String requestedBy, List<OrderItem> itemsToRemove, List<OrderItem> itemsToAdd) {
        this.orderId = orderId;
        this.requestedBy = requestedBy;
        this.itemsToRemove = itemsToRemove;
        this.itemsToAdd = itemsToAdd;
    }
    
    /**
     * Gets the order ID.
     * @return the order ID
     */
    public String getOrderId() {
        return orderId;
    }
    
    /**
     * Gets the list of items to remove.
     * @return the items to remove
     */
    public List<OrderItem> getItemsToRemove() {
        return itemsToRemove;
    }
    
    /**
     * Gets the list of items to add.
     * @return the items to add
     */
    public List<OrderItem> getItemsToAdd() {
        return itemsToAdd;
    }
}
