package com.exalt.it.belair.domain.order.usecases;

import com.exalt.it.belair.domain.order.model.Order;
import com.exalt.it.belair.domain.order.model.OrderChangeRequest;
import com.exalt.it.belair.domain.order.model.OrderItem;
import com.exalt.it.belair.domain.order.events.OrderChangeApprovedEvent;
import com.exalt.it.belair.domain.order.ports.out.IOrderRepository;
import com.exalt.it.belair.domain.order.ports.out.IChangeRequestRepository;
import com.exalt.it.belair.domain.order.ports.out.IEventPublisher;
import com.exalt.it.belair.domain.order.ports.out.IItemTransferService;
import java.time.LocalDateTime;

/**
 * Use case for approving an order change request.
 * Handles removing prepared items (with transfer validation) and adding new items to an order.
 */
public class ApproveOrderChangeUseCase {
    private final IOrderRepository orderRepository;
    private final IChangeRequestRepository changeRequestRepository;
    private final IEventPublisher eventPublisher;
    private final IItemTransferService itemTransferService;
    
    /**
     * Constructs an ApproveOrderChangeUseCase.
     * 
     * @param orderRepository port for persisting orders
     * @param changeRequestRepository port for retrieving change requests
     * @param eventPublisher port for publishing domain events
     * @param itemTransferService port for validating item transfers
     */
    public ApproveOrderChangeUseCase(
            IOrderRepository orderRepository,
            IChangeRequestRepository changeRequestRepository,
            IEventPublisher eventPublisher,
            IItemTransferService itemTransferService) {
        this.orderRepository = orderRepository;
        this.changeRequestRepository = changeRequestRepository;
        this.eventPublisher = eventPublisher;
        this.itemTransferService = itemTransferService;
    }
    
    /**
     * Approves an order change by applying the requested modifications.
     * Validates that prepared items can be transferred before removal.
     * 
     * @param orderId the ID of the order to change
     * @param bartenderId the ID of the bartender approving the change
     * @throws IllegalArgumentException if order or change request not found
     * @throws IllegalStateException if a prepared item cannot be transferred
     */
    public void approveChange(String orderId, String bartenderId) {
        // Load the order
        // TODO: Replace IllegalArgumentException with OrderNotFoundException when implementing Order Not Found scenario
        Order order = orderRepository.findById(orderId)
            .orElseThrow(() -> new IllegalArgumentException("Order not found: " + orderId));
        
        // Load the change request
        // TODO: Replace IllegalArgumentException with ChangeRequestNotFoundException when implementing Change Request Not Found scenario
        OrderChangeRequest changeRequest = changeRequestRepository.findByOrderId(orderId)
            .orElseThrow(() -> new IllegalArgumentException("Change request not found for order: " + orderId));
        
        // Verify that all prepared items to remove can be transferred
        for (OrderItem itemToRemove : changeRequest.getItemsToRemove()) {
            if (itemToRemove.isPrepared()) {
                boolean canTransfer = itemTransferService.canTransferPreparedItem(itemToRemove.getId(), itemToRemove.getType());
                if (!canTransfer) {
                    // TODO: Replace IllegalStateException with PreparedItemCannotBeTransferredException when implementing Item Transfer Failure scenario
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
