package com.exalt.it.belair.domain.order.model;

import java.util.List;

public class Order {
    private final String orderId;
    private final String festivalGoerId;
    private final List<OrderItem> items;
    private final OrderStatus status;

    public Order(String orderId, String festivalGoerId, List<OrderItem> items, OrderStatus status) {
        this.orderId = orderId;
        this.festivalGoerId = festivalGoerId;
        this.items = items;
        this.status = status;
    }

    public String getOrderId() {
        return orderId;
    }

    public OrderStatus getStatus() {
        return status;
    }

    public List<OrderItem> getItems() {
        return items;
    }

    public int getDrinkTokenCost() {
        int cost = 0;
        for (OrderItem item : items) {
            cost += item.getDrinkTokenCost();
        }
        return cost;
    }

    public int getSnackTokenCost() {
        int cost = 0;
        for (OrderItem item : items) {
            cost += item.getSnackTokenCost();
        }
        return cost;
    }
}

