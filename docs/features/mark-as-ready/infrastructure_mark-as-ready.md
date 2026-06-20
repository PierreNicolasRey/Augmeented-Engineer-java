# Infrastructure: Mark an Order as Ready Persistence

**Context**
The Infrastructure module must persist order ready state to the database and publish the readiness event. This is a simple update operation with minimal complexity.

**Problem**
The Infrastructure layer requires repository implementations to update order status and persist the ready timestamp while dispatching the readiness notification event.

**Acceptance Criteria**
- [ ] `OrderEntity` JPA entity updated:
  - Field: `readyAt: Instant` (new field for ready timestamp)
  - Existing status field with READY value
  
- [ ] `OrderRepositoryAdapter` extended:
  - Method: `markOrderReady(OrderId): void` (or via `saveOrder`)
  - Update status to READY and set readyAt timestamp
  
- [ ] Event persistence/dispatching:
  - Publish `OrderReadyEvent` via `EventPublisherAdapter`
  
- [ ] Transactional wrapper:
  - Operations wrapped in `@Transactional`
  
- [ ] Database operations:
  - Update `orders.status` to 'READY'
  - Update `orders.ready_at` timestamp
  - Update `orders.updated_at` timestamp
  - Persist event to audit table
  
- [ ] Integration tests verify:
  - Order status updated to READY
  - Timestamps set correctly
  - Event published
  - Query after ready shows correct state

**Implementation Plan**
1. Update `OrderEntity` JPA entity:
   - Add `readyAt: Instant` field

2. Extend `OrderRepositoryAdapter`:
   ```java
   @Transactional
   public void markOrderReady(String orderId) {
     OrderEntity entity = orderJpaRepository.findById(orderId)
       .orElseThrow(OrderNotFoundException::new);
     entity.setStatus(OrderStatus.READY);
     entity.setReadyAt(Instant.now());
     entity.setUpdatedAt(Instant.now());
     orderJpaRepository.save(entity);
   }
   ```

3. Event publishing: via existing `EventPublisherAdapter`

4. Database migration:
   ```sql
   ALTER TABLE orders ADD COLUMN ready_at TIMESTAMP NULL;
   ```

5. Integration tests: verify status, timestamps, event published

**Gherkin Scenarios**
Feature: Mark as Ready Persistence

# === HAPPY PATH ===
Scenario: Mark acknowledged order as ready in database
  Given an OrderEntity with ID "ord-001" and status "ACKNOWLEDGED"
  And the order has estimated_readiness_at timestamp set
  When markOrderReady is called with "ord-001"
  Then the database shows order status = "READY"
  And ready_at timestamp is set to current time (within 1 second)
  And updated_at timestamp is refreshed to current time

Scenario: Event is published when order marked ready
  Given an order "ord-002" with status "ACKNOWLEDGED"
  When the order is marked ready
  Then OrderReadyEvent is dispatched with order ID and READY status

# === ERROR CASES ===
Scenario: Cannot mark non-existent order as ready (404)
  Given an order ID "ord-999" that does not exist in database
  When markOrderReady is called with "ord-999"
  Then an OrderNotFoundException is thrown
  And no rows in the database are modified

Scenario: Cannot mark order as ready if already ready (409)
  Given an OrderEntity with ID "ord-003" and status "READY"
  When markOrderReady is called with "ord-003"
  Then a StateConflictException is thrown
  And the order status remains "READY"
  And ready_at timestamp is NOT modified

Scenario: Cannot mark order as ready if status is PENDING (409)
  Given an OrderEntity with ID "ord-004" and status "PENDING"
  When markOrderReady is called
  Then a StateConflictException is thrown
  And the order status remains "PENDING"

Scenario: Cannot mark order as ready if status is CANCELLED (409)
  Given an OrderEntity with ID "ord-005" and status "CANCELLED"
  When markOrderReady is called
  Then a StateConflictException is thrown
  And the order status remains "CANCELLED"

# === EDGE CASES ===
Scenario: Mark order ready with null order ID (400)
  Given a null order ID parameter
  When markOrderReady is called with null
  Then a ValidationException is thrown
  And no database changes occur

Scenario: Mark order ready with empty string order ID (400)
  Given an empty order ID ""
  When markOrderReady is called with ""
  Then a ValidationException is thrown
  And no database changes occur

# === PERSISTENCE VERIFICATION ===
Scenario: Query after marking ready returns latest state (no stale data)
  Given an order marked ready
  When queried immediately after from the database
  Then status is "READY" (not cached or stale)
  And ready_at is set (not null)
  And updated_at reflects the update time

# === TRANSACTION HANDLING ===
Scenario: Transactional rollback on database constraint violation
  Given an order that violates a constraint during update
  When markOrderReady is called
  Then the transaction rolls back
  And the order status remains in its original state (ACKNOWLEDGED)
  And no partial updates occur

Scenario: Multiple concurrent mark-ready requests are isolated
  Given an order "ord-006" with status "ACKNOWLEDGED"
  When two concurrent requests to markOrderReady("ord-006") are made
  Then one request succeeds (first one wins)
  And the other either:
    - Succeeds with idempotent behavior (status already READY, no error), or
    - Throws StateConflictException (status changed between read and write)
  And the database shows exactly one ready_at timestamp

**Notes**
- Simple state transition; minimal business logic.
- Event notification is async via domain events.
