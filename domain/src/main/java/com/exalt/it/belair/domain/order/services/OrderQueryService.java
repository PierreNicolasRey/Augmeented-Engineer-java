package com.exalt.it.belair.domain.order.services;

import com.exalt.it.belair.domain.order.model.Order;
import com.exalt.it.belair.domain.order.ports.in.OrderQueryServicePort;
import com.exalt.it.belair.domain.order.ports.out.IOrderRepository;
import java.util.Optional;

/**
 * Domain query service for retrieving orders.
 * Implements the read-side of the CQS pattern, delegating to the order repository.
 * This service is a pass-through to the repository port and returns Domain Models.
 */
public class OrderQueryService implements OrderQueryServicePort {

    private final IOrderRepository orderRepository;

    public OrderQueryService(IOrderRepository orderRepository) {
        this.orderRepository = orderRepository;
    }

    @Override
    public Optional<Order> findOrderById(String orderId) {
        return orderRepository.findById(orderId);
    }
}
