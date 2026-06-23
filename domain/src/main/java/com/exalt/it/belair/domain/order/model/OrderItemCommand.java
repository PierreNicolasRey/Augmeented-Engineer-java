package com.exalt.it.belair.domain.order.model;

public class OrderItemCommand {
    private final String itemType;
    private final String itemSubtype;
    private final int quantity;

    public OrderItemCommand(String itemType, String itemSubtype, int quantity) {
        this.itemType = itemType;
        this.itemSubtype = itemSubtype;
        this.quantity = quantity;
    }

    public String getItemType() {
        return itemType;
    }

    public String getItemSubtype() {
        return itemSubtype;
    }

    public int getQuantity() {
        return quantity;
    }
}
