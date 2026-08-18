package com.exalt.it.belair.domain.order.ports.in;

import com.exalt.it.belair.domain.order.model.Order;
import java.util.Optional;

/**
 * Inbound port for querying orders.
 * Defines the read-side contract for order retrieval, following the CQS pattern.
 * Implementations are responsible for efficient, read-optimized access to order data.
 */
public interface OrderQueryServicePort {

    /**
     * Finds an order by its unique identifier.
     *
     * @param orderId the unique identifier of the order to retrieve
     * @return an Optional containing the order if found, or empty if not found
     */
    Optional<Order> findOrderById(String orderId);
}
