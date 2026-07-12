package com.exalt.it.belair.domain.order.model;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Represents an order placed by a festival goer.
 * An Order is an aggregate root that encapsulates the items ordered and their state.
 */
public class Order {
    private final String orderId;
    private final String festivalGoerId;
    private final List<OrderItem> items;
    private OrderStatusEnum status;
    private int reservedDrinkTokens = 0;
    private int reservedSnackTokens = 0;
    private Instant updatedAt;
    private LocalDateTime estimatedReadinessAt;

    /**
     * Constructs an Order with the specified parameters.
     * 
     * @param orderId the unique identifier of the order
     * @param festivalGoerId the ID of the festival goer who placed the order
     * @param items the list of items in the order
     * @param status the current status of the order
     */
    public Order(String orderId, String festivalGoerId, List<OrderItem> items, OrderStatusEnum status) {
        this.orderId = orderId;
        this.festivalGoerId = festivalGoerId;
        this.items = items;
        this.status = status;
    }

    /**
     * Gets the order ID.
     * @return the order ID
     */
    public String getOrderId() {
        return orderId;
    }

    /**
     * Gets the order ID (alias for getOrderId()).
     * @return the order ID
     */
    public String getId() {
        return orderId;
    }

    /**
     * Gets the festival goer ID who placed this order.
     * @return the festival goer ID
     */
    public String getFestivalGoerId() {
        return festivalGoerId;
    }

    /**
     * Gets the current status of this order.
     * @return the order status
     */
    public OrderStatusEnum getStatus() {
        return status;
    }

    /**
     * Sets the status of this order.
     * Used during order state transitions (e.g., to CANCELLED).
     * 
     * @param status the new status
     */
    public void setStatus(OrderStatusEnum status) {
        this.status = status;
    }

    /**
     * Gets the items in this order.
     * @return the list of items
     */
    public List<OrderItem> getItems() {
        return new ArrayList<>(items);
    }

    /**
     * Adds an item to this order.
     * 
     * @param item the item to add
     */
    public void addItem(OrderItem item) {
        items.add(item);
    }

    /**
     * Removes an item from this order by its ID.
     * 
     * @param itemId the ID of the item to remove
     */
    public void removeItem(String itemId) {
        items.removeIf(item -> itemId.equals(item.getId()));
    }

    /**
     * Sets the estimated readiness time for this order.
     * 
     * @param estimatedReadinessAt the estimated time when the order will be ready
     */
    public void setEstimatedReadinessAt(LocalDateTime estimatedReadinessAt) {
        this.estimatedReadinessAt = estimatedReadinessAt;
    }

    /**
     * Gets the estimated readiness time for this order.
     * 
     * @return the estimated readiness time, or null if not set
     */
    public LocalDateTime getEstimatedReadinessAt() {
        return estimatedReadinessAt;
    }

    /**
     * Calculates the total drink token cost for all items in this order.
     * 
     * @return the total drink token cost
     */
    public int getDrinkTokenCost() {
        int cost = 0;
        for (OrderItem item : items) {
            cost += item.getDrinkTokenCost();
        }
        return cost;
    }

    /**
     * Calculates the total snack token cost for all items in this order.
     * 
     * @return the total snack token cost
     */
    public int getSnackTokenCost() {
        int cost = 0;
        for (OrderItem item : items) {
            cost += item.getSnackTokenCost();
        }
        return cost;
    }

    /**
     * Gets the number of reserved drink tokens for this order.
     * @return the reserved drink tokens
     */
    public int getReservedDrinkTokens() {
        return reservedDrinkTokens;
    }

    /**
     * Gets the number of reserved snack tokens for this order.
     * @return the reserved snack tokens
     */
    public int getReservedSnackTokens() {
        return reservedSnackTokens;
    }

    /**
     * Sets the reserved tokens for this order.
     * Used for tracking which tokens are associated with this order during cancellation.
     * 
     * @param drinkTokens the number of reserved drink tokens
     * @param snackTokens the number of reserved snack tokens
     */
    public void setReservedTokens(int drinkTokens, int snackTokens) {
        this.reservedDrinkTokens = drinkTokens;
        this.reservedSnackTokens = snackTokens;
    }

    /**
     * Gets the timestamp when this order was last updated.
     * @return the last update timestamp
     */
    public Instant getUpdatedAt() {
        return updatedAt;
    }

    /**
     * Sets the timestamp when this order was last updated.
     * Used to track when cancellation occurred.
     * 
     * @param timestamp the update timestamp
     */
    public void setUpdatedAt(Instant timestamp) {
        this.updatedAt = timestamp;
    }
}
