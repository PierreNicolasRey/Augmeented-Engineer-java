package com.exalt.it.belair.domain.order.events;

/**
 * Domain event published when an order change has been approved.
 * This event signals that an order's items have been updated.
 */
public class OrderChangeApprovedEvent {
    private final String orderId;
    private final String festivalGoerId;
    
    /**
     * Constructs an OrderChangeApprovedEvent.
     * 
     * @param orderId the ID of the order that was changed
     * @param festivalGoerId the ID of the festival goer who owns the order
     */
    public OrderChangeApprovedEvent(String orderId, String festivalGoerId) {
        this.orderId = orderId;
        this.festivalGoerId = festivalGoerId;
    }
    
    /**
     * Gets the order ID.
     * @return the order ID
     */
    public String getOrderId() {
        return orderId;
    }
    
    /**
     * Gets the festival goer ID.
     * @return the festival goer ID
     */
    public String getFestivalGoerId() {
        return festivalGoerId;
    }
}
