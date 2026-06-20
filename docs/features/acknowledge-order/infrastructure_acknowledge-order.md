# Infrastructure: Acknowledge an Order Persistence

**Context**
The Infrastructure module must persist order acknowledgements and token consumption to the database atomically. When an order is acknowledged, its status transitions to ACKNOWLEDGED, the estimated readiness time is stored, and the festival goer's token balance is updated to reflect permanent token consumption (not just unreservation). Additionally, the domain event `OrderAcknowledgedEvent` must be persisted or dispatched through a messaging system for the notification handler.

**Problem**
The Infrastructure layer requires repository implementations to handle order status and readiness time updates, token consumption across token balance records, and event publishing/persistence while maintaining transactional consistency.

**Acceptance Criteria**
- [ ] `OrderEntity` JPA entity updated:
  - Field: `status: String` (enum with ACKNOWLEDGED state)
  - Field: `estimatedReadinessAt: Instant` (new, auto-calculated)
  - Field: `acknowledgedAt: Instant` (timestamp of acknowledgement)
  - Field: `updatedAt: Instant` (auto-updated on acknowledge)
  
- [ ] `TokenBalanceEntity` JPA entity has consumption support:
  - Fields: `totalDrinkTokens`, `totalSnackTokens`, `reservedDrinkTokens`, `reservedSnackTokens`
  - Method or logic: `consumeTokens(drinkTokens, snackTokens)` decrements both total and reserved
  - Logic: `available = total - reserved` (calculated, after consumption)
  
- [ ] `OrderRepositoryAdapter` extended:
  - Method: `acknowledgeOrder(OrderId, EstimatedReadinessTime): void` (or via `saveOrder` with status update)
  - Load order, update status to ACKNOWLEDGED, set estimated readiness, persist
  
- [ ] `TokenBalanceRepositoryAdapter` extended (from Feature 1):
  - Method: `consumeTokens(FestivalGoerId, drinkTokens, snackTokens): void`
  - Decrement total tokens (not just reserved): `totalDrinkTokens -= drinkTokens`
  - Also decrement reserved by same amount: `reservedDrinkTokens -= drinkTokens`
  - Validate no negative total or reserved values
  - Persist updated balance
  
- [ ] `OrderJpaRepository` Spring Data repository:
  - Method: `findById(String): Optional<OrderEntity>` (existing)
  - Ensure status and estimated readiness fields are properly persisted
  
- [ ] `TokenBalanceJpaRepository` Spring Data repository (from Feature 1):
  - Method: `findByFestivalGoerId(String): Optional<TokenBalanceEntity>`
  - Method: `save(TokenBalanceEntity): TokenBalanceEntity`
  
- [ ] Event persistence/dispatching:
  - `EventPublisherAdapter` implements Domain port `EventPublisherPort`
  - Method: `publish(OrderAcknowledgedEvent): void`
  - Persist event to database (event store or audit log) for audit trail
  - Dispatch to messaging system (e.g., Kafka, RabbitMQ, or local application event)
  
- [ ] Transactional wrapper:
  - All acknowledgement operations wrapped in `@Transactional`
  - Atomicity: order acknowledged AND tokens consumed AND event published, or all fail
  
- [ ] Exception handling:
  - `DataIntegrityViolationException` caught if constraints violated
  - Meaningful error messages for failures
  
- [ ] Database operations:
  - Update `orders.status` to 'ACKNOWLEDGED'
  - Update `orders.estimated_readiness_at` with calculated time
  - Update `orders.acknowledged_at` with current timestamp
  - Update `orders.updated_at` timestamp
  - Decrement both `token_balances.total_drink_tokens` and `token_balances.reserved_drink_tokens`
  - Decrement both `token_balances.total_snack_tokens` and `token_balances.reserved_snack_tokens`
  - Validate no negative token values (CHECK constraints)
  - Insert event record to event table (if event sourcing used)
  
- [ ] Integration tests verify:
  - Order status updated to ACKNOWLEDGED
  - Estimated readiness time persisted correctly
  - Token balance reflects consumed tokens (both total and reserved decreased)
  - Transactional rollback on constraint violation
  - Cannot consume more tokens than exist (validation)
  - Event is published/dispatched
  - Query after acknowledgement shows correct state

**Implementation Plan**
1. Update `OrderEntity` JPA entity:
   - Add `estimatedReadinessAt: Instant` field with @Column
   - Add `acknowledgedAt: Instant` field with @Column
   - Existing `updatedAt` field (set by @PreUpdate)

2. Ensure `TokenBalanceEntity` JPA entity has fields:
   - `totalDrinkTokens: int`
   - `totalSnackTokens: int`
   - `reservedDrinkTokens: int`
   - `reservedSnackTokens: int`
   - CHECK constraints: reserved <= total for both token types

3. Create/Extend `OrderRepositoryAdapter` in persistence:
   - Reuse existing `saveOrder(Order)` method for status update
   - Or create `acknowledgeOrder(OrderId, estimatedReadinessTime): void`
   
   ```java
   @Transactional
   public void acknowledgeOrder(String orderId, Instant estimatedReadinessAt) {
     OrderEntity entity = orderJpaRepository.findById(orderId)
       .orElseThrow(OrderNotFoundException::new);
     entity.setStatus(OrderStatus.ACKNOWLEDGED);
     entity.setEstimatedReadinessAt(estimatedReadinessAt);
     entity.setAcknowledgedAt(Instant.now());
     entity.setUpdatedAt(Instant.now());
     orderJpaRepository.save(entity);
   }
   ```

4. Create/Extend `TokenBalanceRepositoryAdapter` in persistence:
   - Reuse existing `saveTokenBalance(TokenBalance)` method
   - Or create `consumeTokens(FestivalGoerId, drinkTokens, snackTokens): void`
   
   ```java
   @Transactional
   public void consumeTokens(String festivalGoerId, int drinkTokens, int snackTokens) {
     TokenBalanceEntity entity = tokenBalanceJpaRepository.findByFestivalGoerId(festivalGoerId)
       .orElseThrow(FestivalGoerNotFoundException::new);
     
     // Validate we have enough tokens
     if (entity.getTotalDrinkTokens() < drinkTokens 
         || entity.getTotalSnackTokens() < snackTokens) {
       throw new InvalidTokenAmountException("Cannot consume more tokens than available");
     }
     
     // Consume both total and reserved (reserved should equal consumed amount)
     entity.setTotalDrinkTokens(entity.getTotalDrinkTokens() - drinkTokens);
     entity.setTotalSnackTokens(entity.getTotalSnackTokens() - snackTokens);
     entity.setReservedDrinkTokens(entity.getReservedDrinkTokens() - drinkTokens);
     entity.setReservedSnackTokens(entity.getReservedSnackTokens() - snackTokens);
     
     tokenBalanceJpaRepository.save(entity);
   }
   ```

5. Create `EventPublisherAdapter` in persistence/messaging:
   - Implements Domain port `EventPublisherPort`
   
   ```java
   @Component
   public class EventPublisherAdapter implements EventPublisherPort {
     @Autowired
     private ApplicationEventPublisher applicationEventPublisher;
     
     @Autowired
     private EventRepositoryJpa eventRepository; // For audit trail
     
     @Override
     @Transactional
     public void publish(OrderAcknowledgedEvent event) {
       // Persist event to audit table
       EventEntity eventEntity = new EventEntity();
       eventEntity.setEventType("ORDER_ACKNOWLEDGED");
       eventEntity.setAggregateId(event.orderId());
       eventEntity.setAggregateType("Order");
       eventEntity.setPayload(serializeToJson(event));
       eventEntity.setPublishedAt(Instant.now());
       eventRepository.save(eventEntity);
       
       // Dispatch to application event system (or message broker)
       applicationEventPublisher.publishEvent(event);
     }
   }
   ```

6. Create Spring Data repositories:
   - `OrderJpaRepository extends JpaRepository<OrderEntity, String>`
   - `TokenBalanceJpaRepository extends JpaRepository<TokenBalanceEntity, String>`
     - Custom method: `findByFestivalGoerId(String): Optional<TokenBalanceEntity>`
   - `EventRepositoryJpa extends JpaRepository<EventEntity, Long>` (for audit)

7. Create mappers (likely reuse from Feature 6):
   - `OrderEntityMapper.toDomain()` already handles status field
   - `TokenBalanceEntityMapper.toDomain()` already handles balance fields

8. Update database schema:
   
   ```sql
   -- Add estimated_readiness_at and acknowledged_at to orders table
   ALTER TABLE orders 
   ADD COLUMN estimated_readiness_at TIMESTAMP NULL,
   ADD COLUMN acknowledged_at TIMESTAMP NULL;
   
   -- Add constraints to token_balances
   ALTER TABLE token_balances 
   ADD CONSTRAINT chk_reserved_drink_tokens 
   CHECK (reserved_drink_tokens >= 0 AND reserved_drink_tokens <= total_drink_tokens);
   
   ALTER TABLE token_balances 
   ADD CONSTRAINT chk_reserved_snack_tokens 
   CHECK (reserved_snack_tokens >= 0 AND reserved_snack_tokens <= total_snack_tokens);
   
   -- Create event audit table for event sourcing (optional)
   CREATE TABLE events (
     id BIGINT AUTO_INCREMENT PRIMARY KEY,
     event_type VARCHAR(50) NOT NULL,
     aggregate_id VARCHAR(50) NOT NULL,
     aggregate_type VARCHAR(50) NOT NULL,
     payload JSON NOT NULL,
     published_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
   );
   
   CREATE INDEX idx_events_aggregate ON events(aggregate_id, aggregate_type);
   CREATE INDEX idx_events_published_at ON events(published_at);
   ```

9. Create integration tests:
   
   - Test acknowledge order updates status and timestamps
   - Test token consumption decrements both total and reserved
   - Test estimated readiness time is persisted
   - Test event is published/persisted
   - Test transactional rollback on constraint violation
   - Test cannot consume more tokens than available
   - Test multiple orders - acknowledge one, others unaffected
   - Test query after acknowledgement shows correct state

10. Ensure transactional consistency:
    - Wrap service method in `@Transactional`
    - All three operations (order, token balance, event) in same transaction
    - Cascade: if event publication fails, entire transaction rolls back

**Gherkin Scenarios**
Feature: Acknowledge Order Persistence

Scenario: Acknowledge order and persist status change
  Given an OrderEntity with ID "ord-001" and status "PENDING"
  And a TokenBalanceEntity with 6 drink tokens total, 1 reserved
  When acknowledgeOrder is called with "ord-001" and estimated time 2 minutes
  And consumeTokens is called with 1 drink, 0 snack
  Then the database shows order status = "ACKNOWLEDGED"
  And order.estimated_readiness_at is set to approximately now + 2 minutes
  And order.acknowledged_at is set to approximately now

Scenario: Token consumption decrements total and reserved
  Given a TokenBalanceEntity with 6 drink total, 2 reserved
  When consumeTokens is called with 2 drink tokens
  Then the database shows:
    - total_drink_tokens = 4 (6 - 2)
    - reserved_drink_tokens = 0 (2 - 2)
    - available = 4 (4 - 0)

Scenario: Cannot consume more tokens than available
  Given a TokenBalanceEntity with 3 drink tokens total
  When consumeTokens is called with 5 drink tokens
  Then InvalidTokenAmountException is raised
  And the database shows total = 3 (unchanged)

Scenario: Updated_at timestamp is refreshed
  Given an OrderEntity saved at time T1
  When the order is acknowledged at time T2
  And the database is queried
  Then updated_at is approximately T2

Scenario: Transactional consistency - order, tokens, event all persist
  Given an OrderEntity, TokenBalanceEntity, and event in same transaction
  When all three are updated/created and committed
  Then all three appear in the database
  And rolling back one operation rolls back all three

Scenario: Transactional rollback on constraint violation
  Given an order acknowledgement operation
  When a constraint is violated (e.g., festival goer not found)
  Then the transaction rolls back
  And order status remains PENDING
  And token balance unchanged
  And event not persisted

Scenario: Event is persisted to audit table
  Given an OrderAcknowledgedEvent is published
  When the event handler persists it
  Then the events table contains the event record
  And the event payload includes orderId, festivalGoerId, readiness time

Scenario: Query after acknowledgement returns latest state
  Given an order is acknowledged
  When the order is queried immediately after
  Then the status is "ACKNOWLEDGED" (not cached stale data)
  And estimated_readiness_at and acknowledged_at are set

Scenario: Multiple orders - acknowledge one affects only that order
  Given OrderEntity "ord-001" and "ord-002" in PENDING status
  And TokenBalanceEntity with 6 tokens reserved across both orders
  When "ord-001" is acknowledged
  And consumeTokens is called with 2 tokens
  Then "ord-001" status = "ACKNOWLEDGED"
  And "ord-002" status = "PENDING" (unchanged)
  And total reserved = 4 (6 - 2)

Scenario: Snack token consumption
  Given a TokenBalanceEntity with 3 snack tokens total, 1 reserved
  When consumeTokens is called with 0 drink, 1 snack
  Then the database shows:
    - total_snack_tokens = 2 (3 - 1)
    - reserved_snack_tokens = 0 (1 - 1)
    - total_drink_tokens = 3 (unchanged)

Scenario: Mixed token consumption
  Given a TokenBalanceEntity with 6 drink, 9 snack (3 drink, 2 snack reserved)
  When consumeTokens is called with 2 drink, 1 snack
  Then the database shows:
    - total drink = 4 (6 - 2), reserved drink = 1 (3 - 2)
    - total snack = 8 (9 - 1), reserved snack = 1 (2 - 1)

Scenario: Estimated readiness time calculation and persistence
  Given an order "ord-003" containing 1 premium drink and 1 snack type
  When acknowledgeOrder calculates estimated time = 5 minutes
  And the order is persisted
  Then estimated_readiness_at = approximately now + 5 minutes

**Notes**
- Token consumption decrements both `total` and `reserved` by the same amount (reserved tokens are what get consumed).
- Constraints ensure `reserved <= total` and no negative values.
- Event persistence is optional but recommended for audit trails; can use local ApplicationEventPublisher or message broker.
- This feature builds on existing order and token balance persistence (Features 1 & 2).
- Transactional boundaries ensure consistency across order, token balance, and event.
- Status queries benefit from index on `orders.status` for filtering.
