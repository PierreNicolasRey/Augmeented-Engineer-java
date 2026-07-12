package com.exalt.it.belair.domain.order.ports.out;

/**
 * Outbound port for checking if prepared items can be transferred to other orders.
 * Abstracts the item transfer validation logic from the domain layer.
 * Implementation to be provided by the Infrastructure layer.
 */
public interface IItemTransferService {
    
    /**
     * Checks whether a prepared item can be transferred to another order.
     * 
     * @param itemId the ID of the item to transfer
     * @param itemType the type of the item (DRINK, FOOD)
     * @return true if the item can be transferred, false otherwise
     */
    boolean canTransferPreparedItem(String itemId, String itemType);
}
