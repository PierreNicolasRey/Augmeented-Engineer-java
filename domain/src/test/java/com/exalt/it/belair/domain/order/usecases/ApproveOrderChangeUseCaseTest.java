package com.exalt.it.belair.domain.order.usecases;

import com.exalt.it.belair.domain.order.model.OrderStatusEnum;
import com.exalt.it.belair.domain.order.model.OrderItem;
import com.exalt.it.belair.domain.order.model.Order;
import com.exalt.it.belair.domain.order.model.OrderChangeRequest;
import com.exalt.it.belair.domain.order.events.OrderChangeApprovedEvent;
import com.exalt.it.belair.domain.order.ports.out.IOrderRepository;
import com.exalt.it.belair.domain.order.ports.out.IChangeRequestRepository;
import com.exalt.it.belair.domain.order.ports.out.IEventPublisher;
import com.exalt.it.belair.domain.order.ports.out.IItemTransferService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class ApproveOrderChangeUseCaseTest {
    
    private ApproveOrderChangeUseCase sut;
    private TestOrderRepository orderRepository;
    private TestChangeRequestRepository changeRequestRepository;
    private TestEventPublisher eventPublisher;
    private TestItemTransferService itemTransferService;
    
    @BeforeEach
    void setUp() {
        orderRepository = new TestOrderRepository();
        changeRequestRepository = new TestChangeRequestRepository();
        eventPublisher = new TestEventPublisher();
        itemTransferService = new TestItemTransferService();
        sut = new ApproveOrderChangeUseCase(orderRepository, changeRequestRepository, eventPublisher, itemTransferService);
    }
    
    @Test
    void approveChange_shouldUpdateOrderItems_whenChangingAcknowledgedOrderWithTransferablePreparedItems() {
        // GIVEN an order "ord-001" with status "ACKNOWLEDGED" containing 3 items (2 prepared)
        String orderId = "ord-001";
        String festivalGoerId = "fgv-001";
        List<OrderItem> items = new ArrayList<>();
        items.add(new OrderItem("item-1", "DRINK", "NORMAL_ALCOHOLIC", 1, true));   // prepared
        items.add(new OrderItem("item-2", "DRINK", "NORMAL_ALCOHOLIC", 1, true));   // prepared
        items.add(new OrderItem("item-3", "FOOD", "SNACK", 1, false));  // not prepared
        Order order = new Order(orderId, festivalGoerId, items, OrderStatusEnum.ACKNOWLEDGED);
        orderRepository.save(order);
        
        // And a change request to add 1 new item and remove 1 prepared item
        OrderChangeRequest changeRequest = new OrderChangeRequest(
            orderId,
            festivalGoerId,
            List.of(new OrderItem("item-1", "DRINK", "NORMAL_ALCOHOLIC", 1, true)), // items to remove
            List.of(new OrderItem("item-4", "FOOD", "SNACK", 1, false))  // items to add
        );
        changeRequestRepository.save(changeRequest);
        
        // WHEN the bartender approves the change
        sut.approveChange(orderId, "bartender-001");
        
        // THEN the order items are updated (1 removed, 1 added)
        Order updatedOrder = orderRepository.findById(orderId).orElseThrow();
        assertThat(updatedOrder.getItems()).hasSize(3);
        assertThat(updatedOrder.getItems()).noneMatch(item -> "item-1".equals(item.getId()));
        assertThat(updatedOrder.getItems()).anyMatch(item -> "item-4".equals(item.getId()));
        assertThat(updatedOrder.getEstimatedReadinessAt()).isNotNull();
        List<OrderChangeApprovedEvent> publishedEvents = eventPublisher.getPublishedEvents();
        assertThat(publishedEvents).hasSize(1);
        assertThat(publishedEvents.get(0).getOrderId()).isEqualTo(orderId);
        assertThat(publishedEvents.get(0).getFestivalGoerId()).isEqualTo(festivalGoerId);
    }
    
    // ============ TEST DOUBLES ============
    
    // ============ TEST DOUBLES ============
    
    static class TestOrderRepository implements IOrderRepository {
        private final List<Order> store = new ArrayList<>();
        
        @Override
        public Order save(Order order) {
            store.removeIf(o -> o.getId().equals(order.getId()));
            store.add(order);
            return order;
        }
        
        @Override
        public Optional<Order> findById(String orderId) {
            return store.stream()
                .filter(o -> orderId.equals(o.getId()))
                .findFirst();
        }
    }
    
    static class TestChangeRequestRepository implements IChangeRequestRepository {
        private final List<OrderChangeRequest> store = new ArrayList<>();
        
        @Override
        public OrderChangeRequest save(OrderChangeRequest changeRequest) {
            store.removeIf(cr -> cr.getOrderId().equals(changeRequest.getOrderId()));
            store.add(changeRequest);
            return changeRequest;
        }
        
        @Override
        public Optional<OrderChangeRequest> findByOrderId(String orderId) {
            return store.stream()
                .filter(cr -> orderId.equals(cr.getOrderId()))
                .findFirst();
        }
    }
    
    static class TestEventPublisher implements IEventPublisher {
        private final List<OrderChangeApprovedEvent> publishedEvents = new ArrayList<>();
        
        @Override
        public void publish(Object event) {
            if (event instanceof OrderChangeApprovedEvent) {
                publishedEvents.add((OrderChangeApprovedEvent) event);
            }
        }
        
        List<OrderChangeApprovedEvent> getPublishedEvents() {
            return new ArrayList<>(publishedEvents);
        }
    }
    
    static class TestItemTransferService implements IItemTransferService {
        @Override
        public boolean canTransferPreparedItem(String itemId, String itemType) {
            // Minimal: assume all prepared items can be transferred in this test scenario
            return true;
        }
    }
}
