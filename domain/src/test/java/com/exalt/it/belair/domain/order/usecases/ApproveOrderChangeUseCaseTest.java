package com.exalt.it.belair.domain.order.usecases;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.time.LocalDateTime;
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
        Order order = new Order(orderId, festivalGoerId, OrderStatusEnum.ACKNOWLEDGED);
        order.addItem(new OrderItem("item-1", "DRINK", true));   // prepared
        order.addItem(new OrderItem("item-2", "DRINK", true));   // prepared
        order.addItem(new OrderItem("item-3", "SNACK", false));  // not prepared
        orderRepository.save(order);
        
        // And a change request to add 1 new item and remove 1 prepared item
        OrderChangeRequest changeRequest = new OrderChangeRequest(
            orderId,
            festivalGoerId,
            List.of(new OrderItem("item-1", "DRINK", true)), // items to remove
            List.of(new OrderItem("item-4", "SNACK", false))  // items to add
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
    
    // ============ INNER CLASSES: ALL PRODUCTION CODE BELOW ============
    
    enum OrderStatusEnum {
        PENDING,
        ACKNOWLEDGED
    }
    
    static class OrderItem {
        private final String id;
        private final String type;
        private final boolean prepared;
        
        OrderItem(String id, String type, boolean prepared) {
            this.id = id;
            this.type = type;
            this.prepared = prepared;
        }
        
        String getId() {
            return id;
        }
        
        String getType() {
            return type;
        }
        
        boolean isPrepared() {
            return prepared;
        }
    }
    
    static class Order {
        private final String id;
        private final String festivalGoerId;
        private OrderStatusEnum status;
        private List<OrderItem> items;
        private LocalDateTime estimatedReadinessAt;
        
        Order(String id, String festivalGoerId, OrderStatusEnum status) {
            this.id = id;
            this.festivalGoerId = festivalGoerId;
            this.status = status;
            this.items = new ArrayList<>();
        }
        
        String getId() {
            return id;
        }
        
        String getFestivalGoerId() {
            return festivalGoerId;
        }
        
        void addItem(OrderItem item) {
            items.add(item);
        }
        
        void removeItem(String itemId) {
            items.removeIf(item -> itemId.equals(item.getId()));
        }
        
        List<OrderItem> getItems() {
            return new ArrayList<>(items);
        }
        
        void setEstimatedReadinessAt(LocalDateTime estimatedReadinessAt) {
            this.estimatedReadinessAt = estimatedReadinessAt;
        }
        
        LocalDateTime getEstimatedReadinessAt() {
            return estimatedReadinessAt;
        }
    }
    
    static class OrderChangeRequest {
        private final String orderId;
        private final String requestedBy;
        private final List<OrderItem> itemsToRemove;
        private final List<OrderItem> itemsToAdd;
        
        OrderChangeRequest(String orderId, String requestedBy, List<OrderItem> itemsToRemove, List<OrderItem> itemsToAdd) {
            this.orderId = orderId;
            this.requestedBy = requestedBy;
            this.itemsToRemove = itemsToRemove;
            this.itemsToAdd = itemsToAdd;
        }
        
        String getOrderId() {
            return orderId;
        }
        
        List<OrderItem> getItemsToRemove() {
            return itemsToRemove;
        }
        
        List<OrderItem> getItemsToAdd() {
            return itemsToAdd;
        }
    }
    
    static class ApproveOrderChangeUseCase {
        private final IOrderRepository orderRepository;
        private final IChangeRequestRepository changeRequestRepository;
        private final IEventPublisher eventPublisher;
        private final IItemTransferService itemTransferService;
        
        ApproveOrderChangeUseCase(IOrderRepository orderRepository, IChangeRequestRepository changeRequestRepository, IEventPublisher eventPublisher, IItemTransferService itemTransferService) {
            this.orderRepository = orderRepository;
            this.changeRequestRepository = changeRequestRepository;
            this.eventPublisher = eventPublisher;
            this.itemTransferService = itemTransferService;
        }
        
        void approveChange(String orderId, String bartenderId) {
            // Load the order
            Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Order not found: " + orderId));
            
            // Load the change request
            OrderChangeRequest changeRequest = changeRequestRepository.findByOrderId(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Change request not found for order: " + orderId));
            
            // Verify that all prepared items to remove can be transferred
            for (OrderItem itemToRemove : changeRequest.getItemsToRemove()) {
                if (itemToRemove.isPrepared()) {
                    boolean canTransfer = itemTransferService.canTransferPreparedItem(itemToRemove.getId(), itemToRemove.getType());
                    if (!canTransfer) {
                        throw new IllegalStateException("Cannot transfer prepared item: " + itemToRemove.getId());
                    }
                }
            }
            
            // Remove items from order
            for (OrderItem itemToRemove : changeRequest.getItemsToRemove()) {
                order.removeItem(itemToRemove.getId());
            }
            
            // Add items to order
            for (OrderItem itemToAdd : changeRequest.getItemsToAdd()) {
                order.addItem(itemToAdd);
            }
            
            // Set estimated readiness time
            order.setEstimatedReadinessAt(LocalDateTime.now());
            
            // Save updated order
            orderRepository.save(order);
            
            // Publish event
            OrderChangeApprovedEvent event = new OrderChangeApprovedEvent(orderId, order.getFestivalGoerId());
            eventPublisher.publish(event);
        }
    }
    
    interface IOrderRepository {
        Order save(Order order);
        Optional<Order> findById(String orderId);
    }
    
    interface IChangeRequestRepository {
        OrderChangeRequest save(OrderChangeRequest changeRequest);
        Optional<OrderChangeRequest> findByOrderId(String orderId);
    }
    
    interface IEventPublisher {
        void publish(Object event);
    }
    
    interface IItemTransferService {
        boolean canTransferPreparedItem(String itemId, String itemType);
    }
    
    static class OrderChangeApprovedEvent {
        private final String orderId;
        private final String festivalGoerId;
        
        OrderChangeApprovedEvent(String orderId, String festivalGoerId) {
            this.orderId = orderId;
            this.festivalGoerId = festivalGoerId;
        }
        
        String getOrderId() {
            return orderId;
        }
        
        String getFestivalGoerId() {
            return festivalGoerId;
        }
    }
    
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
