package com.exalt.it.belair.domain.order.model;

import java.util.List;

/**
 * Represents an order placed by a festival goer.
 * An Order is an aggregate root that encapsulates the items ordered and their state.
 */
public class Order {
    private final String orderId;
    private final String festivalGoerId;
    private final List<OrderItem> items;
    private final OrderStatusEnum status;

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
     * Gets the items in this order.
     * @return the list of items
     */
    public List<OrderItem> getItems() {
        return items;
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
}

