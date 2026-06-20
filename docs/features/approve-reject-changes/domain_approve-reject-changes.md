# Domain: Approve or Reject Order Changes

**Context**
When a festival goer requests changes to an order that is already ACKNOWLEDGED, the bartender must review the request and decide whether to accept or reject it. The change can only be approved if at least one prepared item can be transferred to another order to make room for the requested changes. If approved, the estimated readiness time is recalculated. This handles Feature 6 change requests for non-pending orders.

**Problem**
The Domain module must provide business logic for bartenders to approve or reject change requests on acknowledged orders. Approval requires validation that prepared items can be transferred and must recalculate estimated time based on the new order composition.

**Acceptance Criteria**
- [ ] `ApproveOrderChangeUseCase` created:
  - Method: `execute(orderId, changeRequest, bartenderId): void`
  - Loads acknowledged order
  - Loads change request details
  - Validates at least one prepared item can be transferred
  - Applies the change (add/remove items)
  - Recalculates estimated readiness time
  - Updates order status (remains ACKNOWLEDGED)
  - Publishes `OrderChangeApprovedEvent`
  
- [ ] `RejectOrderChangeUseCase` created:
  - Method: `execute(orderId, rejectReason, bartenderId): void`
  - Loads acknowledged order
  - Rejects the change request
  - Publishes `OrderChangeRejectedEvent` with reason
  
- [ ] Change request validation:
  - Load change request (persisted or in-memory)
  - Extract items to add, items to remove
  - Verify at least one prepared item in the order can be transferred
  - Check that remaining prepared items don't violate constraints
  
- [ ] Item transfer logic:
  - Determine which items are "prepared" (subset of order items)
  - Validate: `numRemovableItems >= 1` or similar logic
  - Track which items can be transferred to other orders
  
- [ ] Estimated time recalculation:
  - Recalculate based on new item composition
  - Update `estimatedReadinessAt` field
  
- [ ] Token balance adjustment (if applicable):
  - If items are removed (refund tokens if not yet consumed)
  - If items are added (reserve/consume additional tokens)
  - **Note**: Since order is ACKNOWLEDGED, tokens may already be consumed
  
- [ ] Order state management:
  - Order remains ACKNOWLEDGED after approval or rejection
  - Status does not change; only items and estimated time change
  - Set `updatedAt` timestamp
  
- [ ] Domain event publication:
  - Publish `OrderChangeApprovedEvent` containing: orderId, festivalGoerId, newEstimatedReadinessAt
  - Publish `OrderChangeRejectedEvent` containing: orderId, festivalGoerId, rejectReason
  
- [ ] Exception handling:
  - `OrderNotFoundException`
  - `OrderChangeRequestNotFoundException` if change request not found
  - `CannotApproveChangeException` if no prepared items can be transferred
  - `OrderNotInAcknowledgedStateException` if order status not ACKNOWLEDGED
  
- [ ] Port definitions in use:
  - `OrderRepository` port: `findOrderById(OrderId)`, `saveOrder(Order)`
  - `OrderChangeRequestRepository` port (new): `findByOrderId(OrderId)`, `saveChangeRequest(ChangeRequest)`
  - `TokenBalanceRepository` port: for token adjustments if needed
  - `EventPublisherPort` port: `publish(OrderChangeApprovedEvent)`, `publish(OrderChangeRejectedEvent)`

**Implementation Plan**
1. Create `ApproveOrderChangeUseCase` in domain/usecases:
   - Field: `OrderRepository orderRepository`
   - Field: `OrderChangeRequestRepository changeRequestRepository`
   - Field: `TokenBalanceRepository tokenBalanceRepository`
   - Field: `EventPublisherPort eventPublisher`
   - Field: `EstimatedTimeCalculator estimatedTimeCalculator` (reuse from Feature 8)
   - Method signature:
     ```java
     public void execute(String orderId, String bartenderId) throws DomainException
     ```

2. Create `RejectOrderChangeUseCase` in domain/usecases:
   - Field: `OrderRepository orderRepository`
   - Field: `OrderChangeRequestRepository changeRequestRepository`
   - Field: `EventPublisherPort eventPublisher`
   - Method signature:
     ```java
     public void execute(String orderId, String rejectReason, String bartenderId) throws DomainException
     ```

3. Create `OrderChangeRequest` Value Object in domain/model:
   - Fields: orderId, requestedItems (add/remove), requestedAt, status (PENDING/APPROVED/REJECTED)
   - Immutable record-like structure

4. Create `PreparedItemsChecker` service in domain/services (or similar name):
   - Method: `canTransferItems(Order): boolean`
   - Logic: Check if at least one item can be transferred
   - Returns true if transfer possible

5. Implement approval logic:
   ```java
   Order order = orderRepository.findOrderById(orderId).orElseThrow();
   if (!order.getStatus().equals(ACKNOWLEDGED)) throw OrderNotInAcknowledgedStateException;
   
   OrderChangeRequest changeRequest = changeRequestRepository.findByOrderId(orderId).orElseThrow();
   if (!PreparedItemsChecker.canTransferItems(order)) throw CannotApproveChangeException;
   
   // Apply changes
   order.removeItems(changeRequest.getItemsToRemove());
   order.addItems(changeRequest.getItemsToAdd());
   
   // Recalculate estimated time
   Duration newEstimatedTime = estimatedTimeCalculator.calculateEstimatedTime(order);
   order.setEstimatedReadinessAt(Instant.now().plus(newEstimatedTime));
   order.setUpdatedAt(Instant.now());
   
   // Adjust tokens if needed (likely not needed as order already ACKNOWLEDGED)
   // ...
   
   // Save and publish event
   orderRepository.saveOrder(order);
   changeRequestRepository.markAsApproved(changeRequest);
   eventPublisher.publish(new OrderChangeApprovedEvent(orderId, order.getFestivalGoerId(), order.getEstimatedReadinessAt()));
   ```

6. Implement rejection logic:
   ```java
   Order order = orderRepository.findOrderById(orderId).orElseThrow();
   OrderChangeRequest changeRequest = changeRequestRepository.findByOrderId(orderId).orElseThrow();
   
   changeRequestRepository.markAsRejected(changeRequest);
   eventPublisher.publish(new OrderChangeRejectedEvent(orderId, order.getFestivalGoerId(), rejectReason));
   ```

7. Create `OrderChangeRequest` entity in domain/model:
   - Fields: id, orderId, itemsToAdd, itemsToRemove, requestedAt, status, requestedBy

8. Create domain events in domain/events:
   - `OrderChangeApprovedEvent`
   - `OrderChangeRejectedEvent`

9. Define exceptions in domain/exceptions:
   - `CannotApproveChangeException`
   - `OrderChangeRequestNotFoundException`
   - `OrderNotInAcknowledgedStateException`

**Gherkin Scenarios**
Feature: Approve or Reject Order Changes

Scenario: Approve change request with transferable prepared items
  Given an order "ord-001" with status "ACKNOWLEDGED" containing 3 items (2 prepared)
  And a change request to add 1 new item and remove 1 prepared item
  And at least 1 item can be transferred to another order
  When the bartender approves the change
  Then the order items are updated (1 removed, 1 added)
  And the estimated readiness time is recalculated
  And OrderChangeApprovedEvent is published

Scenario: Cannot approve change - no transferable items
  Given an order "ord-002" with 3 items (0 prepared, all in progress)
  And a change request to add items
  When the bartender attempts to approve
  Then CannotApproveChangeException is raised
  And the order remains unchanged

Scenario: Reject change request
  Given a change request for order "ord-003"
  When the bartender rejects with reason "Cannot prepare additional items in time"
  Then the change request is marked rejected
  And OrderChangeRejectedEvent is published with the reason
  And the order remains unchanged

Scenario: Cannot approve/reject if order not acknowledged
  Given an order "ord-004" with status "PENDING"
  And a change request exists
  When attempting to approve or reject
  Then OrderNotInAcknowledgedStateException is raised

Scenario: Recalculated estimated time after approval
  Given an order "ord-005" with estimated readiness 5 minutes
  And a change request to add 2 premium drinks
  When the change is approved
  Then the estimated readiness time is recalculated (e.g., 5 + 6 = 11 minutes)
  And estimatedReadinessAt is updated

Scenario: Change request not found
  Given an order "ord-006" with no pending change request
  When attempting to approve or reject
  Then OrderChangeRequestNotFoundException is raised

Scenario: Multiple change requests - approve one
  Given change requests for orders "ord-007a" and "ord-007b"
  When "ord-007a" change is approved
  Then only "ord-007a" is updated
  And "ord-007b" remains unchanged

Scenario: Approve change with multiple prepared items of different types
  Given an order "ord-008" with 4 items: 2 prepared beers, 1 prepared snack, 1 in-progress item
  And a change request to remove 1 beer and add 1 new snack
  When the bartender approves
  Then 1 prepared beer is transferred out
  Then 1 new snack item is added
  And estimated readiness is recalculated based on new composition

Scenario: Cannot transfer prepared items if none are transferable
  Given an order "ord-009" with 5 items all in progress (0 prepared, all cooking)
  And a change request to add 2 items
  When the bartender attempts to approve
  Then CannotApproveChangeException is raised
  And order remains unchanged (no items added/removed)

Scenario: Partial prepared items - only some are transferable
  Given an order "ord-010" with 3 prepared items and 2 in-progress items
  And a change request to add 1 new item (needs 1 slot)
  When the bartender approves
  Then at least 1 prepared item is transferred
  And new item is added
  And order remains ACKNOWLEDGED

Scenario: Concurrent approvals from different bartenders (race condition handling)
  Given an order "ord-011" with 1 pending change request
  And bartender "bart-001" and "bart-002" both attempt approval simultaneously
  When both approvals are submitted concurrently
  Then one approval succeeds (first one wins)
  And the second approval either:
    - Throws OrderChangeRequestNotFoundException (already approved), or
    - Succeeds idempotently with same result
  And order is updated exactly once

Scenario: Reject does not require prepared items
  Given an order "ord-012" with 0 prepared items
  And a change request pending
  When the bartender rejects
  Then OrderChangeRejectedEvent is published
  And order remains ACKNOWLEDGED
  And rejection succeeds regardless of prepared item count

**Notes**
- This feature handles change requests for ACKNOWLEDGED orders (Feature 6 handles PENDING).
- Approval requires at least one prepared item to be transferable.
- Estimated time recalculation uses the same logic as Feature 8's acknowledgement.
- Rejection is simpler: just reject the request and notify the festival goer.
- Order status remains ACKNOWLEDGED; no state transition.
- Domain events decouple approval from festival goer notification.
