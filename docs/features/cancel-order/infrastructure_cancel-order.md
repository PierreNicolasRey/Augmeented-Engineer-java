# Infrastructure: Cancel an Order Persistence

**Context**
The Infrastructure module must persist order cancellations and token unreservations to the database atomically. When an order is cancelled, its status is updated to CANCELLED, and the associated TokenBalance record is updated to reflect the unreserved tokens. Both operations must succeed or fail together within a single transaction.

**Problem**
The Infrastructure layer requires repository implementations to handle order status updates and coordinated token balance adjustments while maintaining transactional consistency across multiple entities.

**Acceptance Criteria**
- [ ] `OrderEntity` JPA entity has cancellation support:
  - Field: `status: String` (enum with CANCELLED state)
  - Field: `updatedAt: Instant` (auto-updated on cancel)
  
- [ ] `TokenBalanceEntity` JPA entity has unreservation support:
  - Fields: `totalDrinkTokens`, `reservedDrinkTokens`, `totalSnackTokens`, `reservedSnackTokens`
  - Logic: `available = total - reserved` (calculated)
  
- [ ] `OrderRepositoryAdapter` extended:
  - Method: `cancelOrder(OrderId): void` (or reuse `saveOrder` for status update)
  - Load order, update status to CANCELLED, persist
  
- [ ] `TokenBalanceRepositoryAdapter` extended (from Feature 1):
  - Method: `unreserveTokens(TokenBalanceId, drinkTokens, snackTokens): void`
  - Decrement reserved amounts
  - Validate no negative reserved values
  - Persist updated balance
  
- [ ] `OrderJpaRepository` Spring Data repository:
  - Method: `findById(String): Optional<OrderEntity>` (existing)
  - Ensure status field is properly persisted
  
- [ ] `TokenBalanceJpaRepository` Spring Data repository (from Feature 1):
  - Method: `findByFestivalGoerId(String): Optional<TokenBalanceEntity>`
  - Method: `save(TokenBalanceEntity): TokenBalanceEntity`
  
- [ ] Transactional wrapper:
  - Repository operations wrapped in `@Transactional`
  - Atomicity: order cancelled AND tokens unreserved, or both fail
  
- [ ] Exception handling:
  - `DataIntegrityViolationException` caught if constraints violated
  - Meaningful error messages for failures
  
- [ ] Database operations:
  - Update `orders.status` to 'CANCELLED'
  - Update `orders.updated_at` timestamp
  - Decrement `token_balances.reserved_drink_tokens` and `token_balances.reserved_snack_tokens`
  - Validate reserved tokens do not go negative
  
- [ ] Integration tests verify:
  - Order status updated to CANCELLED
  - Updated_at timestamp changed
  - Token balance reserved amounts decreased
  - Transactional rollback on constraint violation
  - Cannot unreserve more tokens than currently reserved (validation)
  - Query after cancellation shows correct state

**Implementation Plan**
1. Ensure `OrderEntity` JPA entity has proper fields (from Feature 6):
   - `status: OrderStatus` (enum or string, with CANCELLED state)
   - `updatedAt: Instant` (with @PreUpdate trigger)

2. Ensure `TokenBalanceEntity` JPA entity has fields:
   - `id: String` (@Id)
   - `festivalGoerId: String` (FK to festival_goers)
   - `totalDrinkTokens: int`
   - `reservedDrinkTokens: int`
   - `totalSnackTokens: int`
   - `reservedSnackTokens: int`
   - Validation: reserved <= total (CHECK constraint or JPA validation)

3. Create/Extend `OrderRepositoryAdapter` in persistence:
   - Reuse existing `saveOrder(Order)` method for status update
   - Alternatively, create `updateOrderStatus(OrderId, status): void`
   
   ```java
   @Transactional
   public void cancelOrder(String orderId) {
     OrderEntity entity = orderJpaRepository.findById(orderId)
       .orElseThrow(OrderNotFoundException::new);
     entity.setStatus(OrderStatus.CANCELLED);
     entity.setUpdatedAt(Instant.now());
     orderJpaRepository.save(entity);
   }
   ```

4. Create/Extend `TokenBalanceRepositoryAdapter` in persistence:
   - Reuse existing `saveTokenBalance(TokenBalance)` method
   - Or create `unreserveTokens(FestivalGoerId, drinkTokens, snackTokens): void`
   
   ```java
   @Transactional
   public void unreserveTokens(String festivalGoerId, int drinkTokens, int snackTokens) {
     TokenBalanceEntity entity = tokenBalanceJpaRepository.findByFestivalGoerId(festivalGoerId)
       .orElseThrow(FestivalGoerNotFoundException::new);
     
     // Validate we don't unreserve more than reserved
     if (entity.getReservedDrinkTokens() < drinkTokens 
         || entity.getReservedSnackTokens() < snackTokens) {
       throw new InvalidTokenAmountException("Cannot unreserve more tokens than reserved");
     }
     
     entity.setReservedDrinkTokens(entity.getReservedDrinkTokens() - drinkTokens);
     entity.setReservedSnackTokens(entity.getReservedSnackTokens() - snackTokens);
     tokenBalanceJpaRepository.save(entity);
   }
   ```

5. Ensure Spring Data repositories are available:
   - `OrderJpaRepository extends JpaRepository<OrderEntity, String>`
   - `TokenBalanceJpaRepository extends JpaRepository<TokenBalanceEntity, String>`
     - Custom method: `findByFestivalGoerId(String): Optional<TokenBalanceEntity>`

6. Create integration tests in `infrastructure/test`:
   
   - Test cancel pending order:
     ```java
     @Test
     void testCancelPendingOrder() {
       // Setup
       OrderEntity order = createOrder(PENDING);
       TokenBalanceEntity balance = createBalance(festivalGoerId, 6, 3, 9, 2);
       
       // Execute
       orderRepositoryAdapter.cancelOrder(order.getId());
       tokenBalanceRepositoryAdapter.unreserveTokens(festivalGoerId, 1, 2);
       
       // Verify
       OrderEntity updated = orderJpaRepository.findById(order.getId()).orElseThrow();
       assert updated.getStatus().equals(CANCELLED);
       
       TokenBalanceEntity updatedBalance = tokenBalanceJpaRepository.findByFestivalGoerId(festivalGoerId).orElseThrow();
       assert updatedBalance.getReservedDrinkTokens() == 2; // 3 - 1
       assert updatedBalance.getReservedSnackTokens() == 0;  // 2 - 2
     }
     ```
   
   - Test updated_at timestamp:
     ```java
     @Test
     void testUpdatedAtTimestampChanges() {
       // Setup
       OrderEntity order = createOrder(PENDING);
       Instant beforeCancel = Instant.now();
       
       // Execute
       orderRepositoryAdapter.cancelOrder(order.getId());
       
       // Verify
       OrderEntity updated = orderJpaRepository.findById(order.getId()).orElseThrow();
       assert updated.getUpdatedAt().isAfter(beforeCancel);
     }
     ```
   
   - Test transactional rollback:
     ```java
     @Test
     void testTransactionalRollback() {
       // Setup
       TokenBalanceEntity balance = createBalance(festivalGoerId, 3, 2, 3, 1);
       
       // Attempt to unreserve more than reserved (should fail)
       assertThrows(InvalidTokenAmountException.class, () ->
         tokenBalanceRepositoryAdapter.unreserveTokens(festivalGoerId, 5, 0)
       );
       
       // Verify balance unchanged
       TokenBalanceEntity unchanged = tokenBalanceJpaRepository.findByFestivalGoerId(festivalGoerId).orElseThrow();
       assert unchanged.getReservedDrinkTokens() == 2; // Unchanged
     }
     ```
   
   - Test cascade: multiple orders, cancel one
     ```java
     @Test
     void testCancelOneOrderDoesNotAffectOthers() {
       // Setup
       OrderEntity order1 = createOrder(PENDING, DRINK_RESERVED=2);
       OrderEntity order2 = createOrder(PENDING, DRINK_RESERVED=1);
       TokenBalanceEntity balance = createBalance(festivalGoerId, 6, 3, 9, 0);
       
       // Execute
       orderRepositoryAdapter.cancelOrder(order1.getId());
       tokenBalanceRepositoryAdapter.unreserveTokens(festivalGoerId, 2, 0);
       
       // Verify
       assert orderJpaRepository.findById(order1.getId()).get().getStatus() == CANCELLED;
       assert orderJpaRepository.findById(order2.getId()).get().getStatus() == PENDING;
       
       TokenBalanceEntity updated = tokenBalanceJpaRepository.findByFestivalGoerId(festivalGoerId).orElseThrow();
       assert updated.getReservedDrinkTokens() == 1; // Only order2's 1 token still reserved
     }
     ```

7. Database constraints (SQL):
   
   ```sql
   -- Ensure reserved <= total in token_balances table
   ALTER TABLE token_balances 
   ADD CONSTRAINT chk_reserved_drink_tokens 
   CHECK (reserved_drink_tokens >= 0 AND reserved_drink_tokens <= total_drink_tokens);
   
   ALTER TABLE token_balances 
   ADD CONSTRAINT chk_reserved_snack_tokens 
   CHECK (reserved_snack_tokens >= 0 AND reserved_snack_tokens <= total_snack_tokens);
   
   -- Index on order status for quick lookup of PENDING orders
   CREATE INDEX idx_orders_status ON orders(status);
   ```

8. Mapper considerations (reuse from Feature 6):
   - `OrderEntityMapper.toDomain()` already handles status field
   - `TokenBalanceEntityMapper.toDomain()` already handles balance fields

**Gherkin Scenarios**
Feature: Cancel Order Persistence

Scenario: Cancel pending order and update database
  Given an OrderEntity with ID "ord-001" and status "PENDING"
  And a TokenBalanceEntity with reserved 3 drink tokens
  When cancelOrder is called with "ord-001"
  And unreserveTokens is called with 1 drink, 0 snack
  Then the database shows order status = "CANCELLED"
  And token balance shows reserved drink = 2 (3 - 1)

Scenario: Updated_at timestamp is refreshed
  Given an OrderEntity saved at time T1
  When the order is cancelled at time T2
  And the database is queried
  Then updated_at is approximately T2 (not T1)

Scenario: Cannot unreserve more tokens than reserved
  Given a TokenBalanceEntity with reserved 2 drink tokens
  When unreserveTokens is called with 5 drink tokens
  Then InvalidTokenAmountException is raised
  And the database shows reserved = 2 (unchanged)

Scenario: Unreserving exactly the reserved amount
  Given a TokenBalanceEntity with reserved 3 drink, 2 snack tokens
  When unreserveTokens is called with 3 drink, 2 snack tokens
  Then the database shows reserved drink = 0, reserved snack = 0
  And available drink = 6 (total - reserved)
  And available snack = 9 (total - reserved)

Scenario: Transactional consistency - order cancelled with tokens unreserved
  Given an OrderEntity and TokenBalanceEntity in same transaction
  When both are updated (status = CANCELLED, reserved tokens decreased)
  And no constraint violations occur
  Then both persist successfully
  And rolling back one operation rolls back both

Scenario: Transactional rollback on constraint violation
  Given an order cancellation operation
  When a constraint is violated (e.g., festival goer not found)
  Then the transaction rolls back
  And order status remains PENDING
  And token balance unchanged

Scenario: Query after cancellation returns latest state
  Given an order is cancelled
  When the order is queried immediately after
  Then the status is "CANCELLED" (not cached stale data)
  And updated_at reflects the cancellation time

Scenario: Multiple orders - cancel affects only the cancelled order
  Given OrderEntity "ord-001" (reserved 2 drink) and "ord-002" (reserved 1 drink)
  And TokenBalanceEntity with reserved 3 drink total
  When "ord-001" is cancelled
  And unreserveTokens is called with 2 drink
  Then "ord-001" status = "CANCELLED"
  And "ord-002" status = "PENDING" (unchanged)
  And total reserved = 1 (only "ord-002")

Scenario: Cascade constraints on festival goer deletion
  Given an order and token balance for a festival goer
  When the festival goer is deleted (cascade)
  Then both order and token balance are deleted (if FK constraints set)

Scenario: Reserved tokens validation
  Given a TokenBalanceEntity with CHECK constraint: reserved <= total
  When attempting to unreserve more than total tokens
  Then the database rejects the operation (constraint violation)
  And InvalidTokenAmountException is raised in code

**Notes**
- Cancellation is idempotent only at the domain level (use case); at database level, status changes PENDING → CANCELLED (not idempotent).
- Unreservation logic ensures reserved never goes negative (validated before persisting).
- Transactional boundaries ensure order and token balance updates are atomic.
- This feature builds on existing order and token balance persistence (Features 1 & 2).
- Status queries benefit from index on `orders.status` for filtering PENDING vs CANCELLED.
