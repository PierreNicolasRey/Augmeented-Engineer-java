package com.exalt.it.belair.infrastructure.persistence.order;

import com.exalt.it.belair.domain.order.model.Order;
import com.exalt.it.belair.domain.order.ports.out.IOrderRepository;
import org.springframework.stereotype.Repository;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Repository adapter implementing the IOrderRepository port.
 * Bridges domain Order models to persistence layer.
 * Delegates all mapping to OrderMapper to maintain separation of concerns.
 */
@Repository
public class OrderRepositoryAdapter implements IOrderRepository {
    
    private final OrderMapper orderMapper;
    private final OrderJpaRepository jpaRepository;
    
    /**
     * In-memory store for persistence (used when JpaRepository is not available).
     */
    private static final Map<String, OrderJpaEntity> ORDER_STORE = new HashMap<>();
    
    /**
     * Constructs the adapter with mapper and repository (for Spring injection).
     */
    public OrderRepositoryAdapter(OrderMapper orderMapper, OrderJpaRepository jpaRepository) {
        this.orderMapper = orderMapper;
        this.jpaRepository = jpaRepository;
    }
    
    /**
     * Constructs the adapter with default mapper and in-memory repository (for testing).
     */
    public OrderRepositoryAdapter() {
        this(new OrderMapper(), null);
    }
    
    /**
     * Saves an order to the repository.
     * Delegates mapping to OrderMapper, then persists via JpaRepository or in-memory store.
     * 
     * @param order the domain Order to persist
     * @return the saved order (reconstructed from persisted entity)
     */
    @Override
    public Order save(Order order) {
        // Convert domain Order to JPA entity (with items included)
        OrderJpaEntity entity = orderMapper.toEntity(order);
        
        // Persist the entity
        if (jpaRepository != null) {
            jpaRepository.save(entity);
        } else {
            ORDER_STORE.put(entity.getId(), entity);
        }
        
        // Reconstruct and return the domain Order from persisted entity
        return orderMapper.toDomain(entity);
    }
    
    /**
     * Finds an order by its ID.
     * Retrieves entity from repository and reconstructs domain Order via mapper.
     * 
     * @param orderId the order ID to retrieve
     * @return Optional containing the Order if found, empty otherwise
     */
    @Override
    public Optional<Order> findById(String orderId) {
        Optional<OrderJpaEntity> entityOpt;
        
        if (jpaRepository != null) {
            entityOpt = jpaRepository.findById(orderId);
        } else {
            entityOpt = Optional.ofNullable(ORDER_STORE.get(orderId));
        }
        
        // Use mapper to reconstruct complete domain Order from entity
        return entityOpt.map(orderMapper::toDomain);
    }
}