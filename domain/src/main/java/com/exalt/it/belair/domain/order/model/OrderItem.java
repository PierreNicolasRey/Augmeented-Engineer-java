package com.exalt.it.belair.domain.order.model;

/**
 * Represents a single item within an order (drink, food, etc.).
 * This is an immutable value object for the Belair Buvette ordering system.
 */
public class OrderItem {
    private final String itemId;
    private final String itemType;
    private final String itemSubtype;
    private final int quantity;

    /**
     * Constructs an OrderItem with the specified parameters.
     * 
     * @param itemId the unique identifier of the item (may be null for items created via factory methods)
     * @param itemType the type of item (DRINK, FOOD)
     * @param itemSubtype the subtype enum name (e.g., NON_ALCOHOLIC, SNACK)
     * @param quantity the quantity ordered
     */
    public OrderItem(String itemId, String itemType, String itemSubtype, int quantity) {
        this.itemId = itemId;
        this.itemType = itemType;
        this.itemSubtype = itemSubtype;
        this.quantity = quantity;
    }

    /**
     * Factory method to create a drink item with the specified type and quantity.
     * 
     * @param type the type of drink
     * @param quantity the quantity ordered
     * @return a new OrderItem for the drink
     */
    public static OrderItem createDrinkItem(DrinkTypeEnum type, int quantity) {
        return new OrderItem(null, "DRINK", type.name(), quantity);
    }

    /**
     * Factory method to create a food item with the specified type and quantity.
     * 
     * @param type the type of food
     * @param quantity the quantity ordered
     * @return a new OrderItem for the food
     */
    public static OrderItem createFoodItem(FoodTypeEnum type, int quantity) {
        return new OrderItem(null, "FOOD", type.name(), quantity);
    }

    /**
     * Gets the item ID.
     * @return the item ID, or null if not set
     */
    public String getItemId() {
        return itemId;
    }

    /**
     * Gets the item type.
     * @return the item type (DRINK, FOOD)
     */
    public String getItemType() {
        return itemType;
    }

    /**
     * Gets the item subtype.
     * @return the subtype enum name
     */
    public String getItemSubtype() {
        return itemSubtype;
    }

    /**
     * Gets the quantity.
     * @return the quantity ordered
     */
    public int getQuantity() {
        return quantity;
    }

    /**
     * Calculates the drink token cost for this item.
     * Returns 0 if this is not a drink item or if it's a non-alcoholic drink.
     * 
     * @return the drink token cost
     */
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

    /**
     * Calculates the snack token cost for this item.
     * Returns 0 if this is not a food item.
     * 
     * @return the snack token cost
     */
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

