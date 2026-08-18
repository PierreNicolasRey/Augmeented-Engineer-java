package com.exalt.it.belair.domain.order.ports.in;

/**
 * Inbound port for acknowledging a pending order.
 * Bartenders use this use case to signal that preparation has begun,
 * triggering token consumption and estimated readiness time calculation.
 */
public interface AcknowledgeOrderUseCasePort {

    /**
     * Acknowledges a pending order, transitioning it from PENDING to ACKNOWLEDGED state.
     * Consumes the reserved tokens from the festival goer's balance and calculates
     * the estimated preparation time based on the ordered items.
     *
     * @param orderId the unique identifier of the order to acknowledge
     * @throws com.exalt.it.belair.domain.order.exceptions.OrderNotFoundException            if no order exists with the given ID
     * @throws com.exalt.it.belair.domain.order.exceptions.OrderCannotBeAcknowledgedException if the order is not in PENDING state
     * @throws com.exalt.it.belair.domain.order.exceptions.FestivalGoerNotFoundException     if the festival goer associated with the order is not found
     */
    void acknowledgeOrder(String orderId);
}
