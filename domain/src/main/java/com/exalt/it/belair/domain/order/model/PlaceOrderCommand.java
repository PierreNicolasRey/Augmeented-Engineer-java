package com.exalt.it.belair.domain.order.model;

import java.util.List;

public class PlaceOrderCommand {
    private final String festivalGoerId;
    private final List<OrderItemCommand> items;

    public PlaceOrderCommand(String festivalGoerId, List<OrderItemCommand> items) {
        this.festivalGoerId = festivalGoerId;
        this.items = items;
    }

    public String getFestivalGoerId() {
        return festivalGoerId;
    }

    public List<OrderItemCommand> getItems() {
        return items;
    }
}
