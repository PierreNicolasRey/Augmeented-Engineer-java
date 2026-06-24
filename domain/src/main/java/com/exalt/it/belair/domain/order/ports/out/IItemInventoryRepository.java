package com.exalt.it.belair.domain.order.ports.out;

/**
 * Outbound port for querying item inventory information.
 * Abstracts access to the catalog and inventory system from the domain layer.
 * Implementation to be provided by Infrastructure layer adapters.
 */
public interface IItemInventoryRepository {
    
    /**
     * Checks if an item exists in the catalog and has sufficient stock.
     * 
     * @param itemId the unique identifier of the item
     * @param requestedQuantity the quantity requested
     * @return {@code true} if the item exists and has at least the requested quantity in stock;
     *         {@code false} otherwise
     */
    boolean hasItemInStock(String itemId, int requestedQuantity);
    
    /**
     * Gets the available quantity for an item in stock.
     * 
     * @param itemId the unique identifier of the item
     * @return the available quantity for the item, or 0 if item not found or out of stock
     */
    int getAvailableQuantity(String itemId);
}
