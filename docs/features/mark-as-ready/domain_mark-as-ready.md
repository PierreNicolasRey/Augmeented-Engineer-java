# Domain: Mark an Order as Ready

**Context**
When the bartender has finished preparing all items in an order, the order must be marked as READY so the festival goer knows they can pick up their order. This is a simple state transition with a notification. Only ACKNOWLEDGED orders can be marked as READY; this prevents marking orders that haven't been started.

**Problem**
The Domain module must provide business logic to mark acknowledged orders as ready and publish a domain event to notify the festival goer that their order is ready for pickup.

**Acceptance Criteria**
- [ ] `MarkOrderReadyUseCase` created:
  - Method: `execute(orderId): void`
  - Loads order by ID
  - Validates order status is ACKNOWLEDGED
  - Throws `OrderCannotBeMarkedReadyException` if not acknowledged
  
- [ ] Order state transition:
  - Update order status to `READY`
  - Set `readyAt: Instant` timestamp
  - Set `updatedAt` timestamp
  
- [ ] Domain event publication:
  - Publish `OrderReadyEvent` containing:
    - orderId, festivalGoerId, readyAt
  - Event handler triggers festival goer notification
  
- [ ] Exception handling:
  - `OrderNotFoundException` if order not found
  - `OrderCannotBeMarkedReadyException` if order not in ACKNOWLEDGED state
  
- [ ] Port definitions in use:
  - `OrderRepository` port: `findOrderById(OrderId)`, `saveOrder(Order)`
  - `EventPublisherPort` port: `publish(OrderReadyEvent)`

**Implementation Plan**
1. Create `MarkOrderReadyUseCase` in domain/usecases:
   - Field: `OrderRepository orderRepository`
   - Field: `EventPublisherPort eventPublisher`
   - Method signature:
     ```java
     public void execute(String orderId) throws DomainException
     ```

2. Implement validation logic:
   - Load order: `Order order = orderRepository.findOrderById(orderId).orElseThrow(OrderNotFoundException::new)`
   - Validate state: `if (!order.getStatus().equals(OrderStatus.ACKNOWLEDGED)) throw OrderCannotBeMarkedReadyException`

3. Implement order state transition:
   - Set status: `order.setStatus(OrderStatus.READY)`
   - Set ready timestamp: `order.setReadyAt(Instant.now())`
   - Set update timestamp: `order.setUpdatedAt(Instant.now())`

4. Implement domain event publication:
   - Create event: `OrderReadyEvent event = new OrderReadyEvent(order.getId(), order.getFestivalGoerId(), Instant.now())`
   - Publish: `eventPublisher.publish(event)`

5. Implement persistence:
   - Save order: `orderRepository.saveOrder(order)`

6. Define exception in domain/exceptions:
   - `OrderCannotBeMarkedReadyException extends DomainException`

7. Create `OrderReadyEvent` domain event in domain/events:
   - Field: `orderId: String`
   - Field: `festivalGoerId: String`
   - Field: `readyAt: Instant`

**Gherkin Scenarios**
Feature: Mark an Order as Ready

Scenario: Mark acknowledged order as ready
  Given an order "ord-001" with status "ACKNOWLEDGED"
  When the mark ready use case is executed
  Then the order status is changed to "READY"
  And readyAt timestamp is set
  And updatedAt timestamp is set
  And OrderReadyEvent is published

Scenario: Cannot mark pending order as ready
  Given an order "ord-002" with status "PENDING"
  When attempting to mark the order as ready
  Then OrderCannotBeMarkedReadyException is raised
  And the order remains in "PENDING" status

Scenario: Cannot mark already ready order
  Given an order "ord-003" with status "READY"
  When attempting to mark the order as ready again
  Then OrderCannotBeMarkedReadyException is raised

Scenario: Cannot mark cancelled order as ready
  Given an order "ord-004" with status "CANCELLED"
  When attempting to mark the order as ready
  Then OrderCannotBeMarkedReadyException is raised

Scenario: Order not found
  Given an order "ord-999" does not exist
  When attempting to mark order "ord-999" as ready
  Then OrderNotFoundException is raised

Scenario: OrderReadyEvent contains correct data
  Given an order "ord-005" is marked ready at time T1
  When the domain event is published
  Then the event contains:
    - orderId: "ord-005"
    - festivalGoerId: (correct ID)
    - readyAt: approximately T1

**Notes**
- Only ACKNOWLEDGED orders can be marked as READY. Pending or cancelled orders cannot be marked ready.
- This is a simple state transition with minimal logic (unlike acknowledge which calculates time).
- Domain events decouple order readiness from festival goer notification.
