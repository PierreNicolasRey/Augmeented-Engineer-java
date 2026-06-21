package com.exalt.it.belair.domain.order.model;

/**
 * Represents an order placed by a festival goer.
 * An Order is an aggregate root that encapsulates the items ordered and their state.
 */
public class Order {
    private final OrderItem item;
    
    /**
     * Constructs an Order with the specified item.
     * 
     * @param item the item to include in this order
     */
    public Order(OrderItem item) {
        this.item = item;
    }
    
    /**
     * Returns the current status of this order.
     * 
     * @return the order status
     */
    public OrderStatusEnum getStatus() {
        return OrderStatusEnum.PENDING;
    }

    /**
     * Returns the count of items in this order.
     * 
     * @return the number of items
     */
    public int getItemCount() {
        return 1;
    }

    /**
     * Calculates the total drink token cost for all items in this order.
     * 
     * @return the drink token cost
     */
    public int getDrinkTokenCost() {
        if (item.drinkType() == DrinkTypeEnum.NORMAL_ALCOHOLIC) {
            return 1;
        }
        return 0;
    }
}
