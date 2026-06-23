package com.exalt.it.belair.domain.order.model;

/**
 * Represents a single item within an order (drink, food, etc.).
 * This is an immutable value object for the Belair Buvette ordering system.
 */
public class OrderItem {
    private final String itemType;
    private final String itemSubtype;
    private final int quantity;
    private final DrinkTypeEnum drinkType;
    
    /**
     * Private constructor for internal use.
     */
    private OrderItem(String itemType, String itemSubtype, int quantity, DrinkTypeEnum drinkType) {
        this.itemType = itemType;
        this.itemSubtype = itemSubtype;
        this.quantity = quantity;
        this.drinkType = drinkType;
    }
    
    /**
     * Factory method to create a drink item with the specified type and quantity.
     * 
     * @param type the type of drink
     * @param quantity the quantity ordered
     * @return a new OrderItem
     */
    public static OrderItem createDrinkItem(DrinkTypeEnum type, int quantity) {
        throw new UnsupportedOperationException("Not implemented yet");
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
    
    public DrinkTypeEnum getDrinkType() {
        return drinkType;
    }
}
