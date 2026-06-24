package com.exalt.it.belair.domain.order.ports.out;

import com.exalt.it.belair.domain.order.model.Order;
import java.util.Optional;

/**
 * Outbound port for persisting and retrieving orders.
 * Abstracts the persistence mechanism (database, etc.) from the domain layer.
 * Implementation to be provided by Infrastructure layer adapters.
 */
public interface IOrderRepository {
    
    /**
     * Saves an order to the repository.
     * 
     * @param order the order to save
     * @return the saved order (may include generated IDs or timestamps)
     */
    Order save(Order order);
    
    /**
     * Retrieves an order by its ID.
     * 
     * @param orderId the ID of the order to retrieve
     * @return an Optional containing the order if found, otherwise empty
     */
    Optional<Order> findById(String orderId);
}
