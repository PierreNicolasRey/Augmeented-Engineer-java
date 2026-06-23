package com.exalt.it.belair.domain.order.model;

import java.util.List;

/**
 * Represents an order placed by a festival goer.
 * An Order is an aggregate root that encapsulates the items ordered and their state.
 */
public class Order {
    private final List<OrderItem> items;
    
    /**
     * Constructs an Order with the specified items.
     * 
     * @param items the items to include in this order
     */
    public Order(List<OrderItem> items) {
        this.items = items;
    }
    
    /**
     * Constructs an Order with a single item (legacy support).
     * 
     * @param item the item to include in this order
     */
    public Order(OrderItem item) {
        this.items = List.of(item);
    }
    
    /**
     * Returns the current status of this order.
     * 
     * @return the order status
     */
    public OrderStatusEnum getStatus() {
        throw new UnsupportedOperationException("Not implemented yet");
    }

    /**
     * Returns the count of items in this order.
     * 
     * @return the number of items
     */
    public int getItemCount() {
        throw new UnsupportedOperationException("Not implemented yet");
    }

    /**
     * Calculates the total drink token cost for all items in this order.
     * 
     * @return the drink token cost
     */
    public int getDrinkTokenCost() {
        throw new UnsupportedOperationException("Not implemented yet");
    }
    
    /**
     * Calculates the total snack token cost for all items in this order.
     * 
     * @return the snack token cost
     */
    public int getSnackTokenCost() {
        throw new UnsupportedOperationException("Not implemented yet");
    }
    
    /**
     * Gets the items in this order.
     * 
     * @return the list of items
     */
    public List<OrderItem> getItems() {
        return items;
    }
}
