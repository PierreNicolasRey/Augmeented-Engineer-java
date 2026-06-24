package com.exalt.it.belair.domain.order.model;

public class OrderItem {
    private final String itemId;
    private final String itemType;
    private final String itemSubtype;
    private final int quantity;

    public OrderItem(String itemId, String itemType, String itemSubtype, int quantity) {
        this.itemId = itemId;
        this.itemType = itemType;
        this.itemSubtype = itemSubtype;
        this.quantity = quantity;
    }

    public static OrderItem createDrinkItem(DrinkType type, int quantity) {
        return new OrderItem(null, "DRINK", type.name(), quantity);
    }

    public static OrderItem createFoodItem(FoodType type, int quantity) {
        return new OrderItem(null, "FOOD", type.name(), quantity);
    }

    public String getItemId() {
        return itemId;
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

    public int getDrinkTokenCost() {
        if ("DRINK".equals(itemType)) {
            if ("NORMAL_ALCOHOLIC".equals(itemSubtype)) {
                return quantity * 1;
            } else if ("PREMIUM_ALCOHOLIC".equals(itemSubtype)) {
                return quantity * 2;
            }
        }
        return 0;
    }

    public int getSnackTokenCost() {
        if ("FOOD".equals(itemType)) {
            if ("SNACK".equals(itemSubtype)) {
                return quantity * 1;
            } else if ("MEAL".equals(itemSubtype)) {
                return quantity * 3;
            }
        }
        return 0;
    }
}
