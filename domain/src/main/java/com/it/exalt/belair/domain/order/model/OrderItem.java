package com.it.exalt.belair.domain.order.model;

/**
 * Represents a single item within an order (drink, food, etc.).
 * This is an immutable value object for the Belair Buvette ordering system.
 */
public record OrderItem(DrinkTypeEnum drinkType, int quantity) {
    /**
     * Factory method to create a drink item with the specified type and quantity.
     * 
     * @param type the type of drink
     * @param quantity the quantity ordered
     * @return a new OrderItem
     */
    public static OrderItem createDrinkItem(DrinkTypeEnum type, int quantity) {
        return new OrderItem(type, quantity);
    }
}
