package com.exalt.it.belair.infrastructure.persistence.order;

import com.exalt.it.belair.domain.order.model.DrinkTypeEnum;
import com.exalt.it.belair.domain.order.model.Order;
import com.exalt.it.belair.domain.order.model.OrderItem;
import com.exalt.it.belair.domain.order.model.OrderStatusEnum;
import com.exalt.it.belair.domain.order.ports.out.IOrderRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Integration tests for OrderRepositoryAdapter.
 * Tests the persistence layer (Infrastructure) adapter that bridges Domain and JPA.
 * 
 * All production code is in inner classes (RED phase - no src/main/java files).
 * Tests verify that Orders can be saved to and retrieved from the database.
 */
class OrderRepositoryAdapterIntegrationTest {
    
    private OrderRepositoryAdapter adapter;
    
    @BeforeEach
    void setUp() {
        adapter = new OrderRepositoryAdapter();
    }
    
    @Test
    void save_shouldPersistOrderAndRetrieveItCorrectly_whenOrderHasSingleDrinkItem() {
        // GIVEN an Order for festival goer "fgv-001"
        String festivalGoerId = "fgv-001";
        
        // And the order contains 1 normal alcoholic drink
        OrderItem item = OrderItem.createDrinkItem(DrinkTypeEnum.NORMAL_ALCOHOLIC, 1);
        List<OrderItem> items = List.of(item);
        Order order = new Order("order-001", festivalGoerId, items, OrderStatusEnum.PENDING);
        
        // WHEN the order is saved to the database
        Order savedOrder = adapter.save(order);
        
        // THEN the order can be retrieved by ID
        Optional<Order> retrievedOrder = adapter.findById("order-001");
        assertThat(retrievedOrder).isPresent();
        
        // And the order contains 1 item
        assertThat(retrievedOrder.get().getItems()).hasSize(1);
        
        // And the item cost is 1 drink token
        assertThat(retrievedOrder.get().getDrinkTokenCost()).isEqualTo(1);
        
        // And the order status is "PENDING"
        assertThat(retrievedOrder.get().getStatus()).isEqualTo(OrderStatusEnum.PENDING);
    }
    
    // ============ INNER CLASSES: ALL PRODUCTION CODE BELOW (RED PHASE) ============
    
    /**
     * Adapter for IOrderRepository port.
     * Bridges Domain Order models to JPA persistence layer.
     * RED phase: only minimal methods that throw UnsupportedOperationException.
     */
    static class OrderRepositoryAdapter implements IOrderRepository {
        
        /**
         * Constructs adapter (minimal for RED phase).
         */
        OrderRepositoryAdapter() {
        }
        
        @Override
        public Order save(Order order) {
            throw new UnsupportedOperationException("Not implemented yet");
        }
        
        @Override
        public Optional<Order> findById(String orderId) {
            throw new UnsupportedOperationException("Not implemented yet");
        }
    }
    
    /**
     * JPA Entity mapping for Order persistence.
     * Represents the 'orders' database table.
     * RED phase: minimal fields required by test.
     */
    @Entity
    @Table(name = "orders")
    static class OrderJpaEntity {
        @Id
        String id;
        
        @Column(name = "festival_goer_id")
        String festivalGoerId;
        
        @Column
        String status;
    }
}
