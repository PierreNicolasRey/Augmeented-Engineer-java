package com.exalt.it.belair.domain.order.usecases;

import com.exalt.it.belair.domain.order.model.Order;
import com.exalt.it.belair.domain.order.model.OrderItem;

/**
 * Use case for placing an order in the Belair Buvette ordering system.
 * Handles the business logic for creating and persisting new orders.
 */
public class PlaceOrderUseCase {
    
    /**
     * Places an order for a festival goer with the specified items and token balances.
     * 
     * @param festivalGoerId the ID of the festival goer placing the order
     * @param item the item to order
     * @param drinkTokens the number of drink tokens available
     * @param snackTokens the number of snack tokens available
     * @return the created Order
     */
    public Order placeOrder(String festivalGoerId, OrderItem item, int drinkTokens, int snackTokens) {
        return new Order(item);
    }
}
