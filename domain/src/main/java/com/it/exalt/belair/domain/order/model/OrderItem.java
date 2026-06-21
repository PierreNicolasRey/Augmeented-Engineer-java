package com.it.exalt.belair.domain.order.model;

public record OrderItem(DrinkType drinkType, int quantity) {
    public static OrderItem createDrinkItem(DrinkType type, int quantity) {
        throw new UnsupportedOperationException("Not implemented yet");
    }
}
