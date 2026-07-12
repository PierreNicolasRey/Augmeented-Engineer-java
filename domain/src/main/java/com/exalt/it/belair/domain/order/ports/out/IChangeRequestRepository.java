package com.exalt.it.belair.domain.order.ports.out;

import com.exalt.it.belair.domain.order.model.OrderChangeRequest;
import java.util.Optional;

/**
 * Outbound port for persisting and retrieving order change requests.
 * Abstracts the persistence mechanism (database, etc.) from the domain layer.
 * Implementation to be provided by Infrastructure layer adapters.
 */
public interface IChangeRequestRepository {
    
    /**
     * Saves an order change request to the repository.
     * 
     * @param changeRequest the change request to save
     * @return the saved change request
     */
    OrderChangeRequest save(OrderChangeRequest changeRequest);
    
    /**
     * Finds an order change request by order ID.
     * 
     * @param orderId the ID of the order
     * @return an Optional containing the change request if found, empty otherwise
     */
    Optional<OrderChangeRequest> findByOrderId(String orderId);
}
