# Infrastructure: Approve or Reject Order Changes Persistence

**Context**
The Infrastructure module must persist order change approval/rejection decisions and track change requests. This requires storing change requests, updating orders with new item compositions and estimated times, and dispatching events.

**Problem**
The Infrastructure layer requires repositories and event handling to manage change requests, persist approval/rejection decisions, and coordinate order updates with estimated time recalculation.

**Acceptance Criteria**
- [ ] `OrderChangeRequestEntity` JPA entity created:
  - Fields: id, orderId, itemsToAdd (JSON or list), itemsToRemove (list of IDs), status (PENDING/APPROVED/REJECTED), requestedAt, respondedAt
  - Foreign key to orders table
  
- [ ] `OrderChangeRequestRepository` (Domain port) implemented:
  - Method: `findByOrderId(OrderId): Optional<OrderChangeRequest>`
  - Method: `save(OrderChangeRequest): void`
  - Method: `updateChangeRequestStatus(OrderChangeRequest, String status): void`
  
- [ ] `OrderChangeRequestJpaRepository` Spring Data repository:
  - `extends JpaRepository<OrderChangeRequestEntity, Long>`
  - Custom method: `findByOrderId(String): Optional<OrderChangeRequestEntity>`
  
- [ ] `OrderChangeRequestRepositoryAdapter`:
  - Implements Domain port `OrderChangeRequestRepository`
  - Maps between JPA entities and Domain models
  
- [ ] `OrderRepositoryAdapter` extended:
  - Handles order updates with new items and estimated time
  - Called during approval process
  
- [ ] Event persistence:
  - Events published via `EventPublisherAdapter` (existing)
  - `OrderChangeApprovedEvent` and `OrderChangeRejectedEvent` dispatched
  
- [ ] Transactional wrapper:
  - All operations wrapped in `@Transactional`
  - Atomicity: order updated AND change request marked AND event published, or all fail
  
- [ ] Exception handling:
  - `DataIntegrityViolationException` caught if constraints violated
  
- [ ] Database operations:
  - Update `order_change_requests.status` to 'APPROVED' or 'REJECTED'
  - Update `order_change_requests.responded_at` timestamp
  - Update `orders.estimated_readiness_at` (if approved)
  - Update `orders.updated_at` (if approved)
  - Insert/update order items (if approved)
  - Persist event to event table
  
- [ ] Integration tests verify:
  - Change request created and retrieved
  - Approval marks request as APPROVED and updates order
  - Rejection marks request as REJECTED without updating order
  - Events published correctly
  - Query after approval/rejection shows correct state

**Implementation Plan**
1. Create `OrderChangeRequestEntity` JPA entity:
   - `@Entity`
   - `@Table(name = "order_change_requests")`
   - `id: Long` (@Id @GeneratedValue)
   - `orderId: String` (FK to orders)
   - `itemsToAdd: String` (JSON representation)
   - `itemsToRemove: String` (JSON or comma-separated IDs)
   - `status: String` (PENDING, APPROVED, REJECTED)
   - `requestedAt: Instant`
   - `respondedAt: Instant` (nullable)
   - `respondedBy: String` (bartender ID, nullable)

2. Create `OrderChangeRequestJpaRepository`:
   ```java
   public interface OrderChangeRequestJpaRepository extends JpaRepository<OrderChangeRequestEntity, Long> {
     Optional<OrderChangeRequestEntity> findByOrderId(String orderId);
   }
   ```

3. Create Domain model `OrderChangeRequest` Value Object:
   - Fields: id, orderId, itemsToAdd, itemsToRemove, status, requestedAt, respondedAt
   - Immutable

4. Create `OrderChangeRequestRepositoryAdapter`:
   ```java
   @Repository
   public class OrderChangeRequestRepositoryAdapter implements OrderChangeRequestRepository {
     @Autowired
     private OrderChangeRequestJpaRepository jpaRepository;
     
     @Autowired
     private OrderChangeRequestEntityMapper mapper;
     
     @Override
     public Optional<OrderChangeRequest> findByOrderId(String orderId) {
       return jpaRepository.findByOrderId(orderId)
         .map(mapper::toDomain);
     }
     
     @Override
     @Transactional
     public void save(OrderChangeRequest changeRequest) {
       OrderChangeRequestEntity entity = mapper.toEntity(changeRequest);
       jpaRepository.save(entity);
     }
     
     @Override
     @Transactional
     public void updateChangeRequestStatus(OrderChangeRequest changeRequest, String newStatus) {
       OrderChangeRequestEntity entity = jpaRepository.findById(changeRequest.id())
         .orElseThrow();
       entity.setStatus(newStatus);
       entity.setRespondedAt(Instant.now());
       jpaRepository.save(entity);
     }
   }
   ```

5. Create mapper `OrderChangeRequestEntityMapper`:
   ```java
   public class OrderChangeRequestEntityMapper {
     public OrderChangeRequest toDomain(OrderChangeRequestEntity entity) {
       List<OrderItem> itemsToAdd = parseJson(entity.getItemsToAdd());
       List<String> itemsToRemove = parseJson(entity.getItemsToRemove());
       return new OrderChangeRequest(
         entity.getId(),
         entity.getOrderId(),
         itemsToAdd,
         itemsToRemove,
         entity.getStatus(),
         entity.getRequestedAt(),
         entity.getRespondedAt()
       );
     }
     
     public OrderChangeRequestEntity toEntity(OrderChangeRequest changeRequest) {
       OrderChangeRequestEntity entity = new OrderChangeRequestEntity();
       entity.setId(changeRequest.id());
       entity.setOrderId(changeRequest.orderId());
       entity.setItemsToAdd(toJson(changeRequest.itemsToAdd()));
       entity.setItemsToRemove(toJson(changeRequest.itemsToRemove()));
       entity.setStatus(changeRequest.status());
       entity.setRequestedAt(changeRequest.requestedAt());
       entity.setRespondedAt(changeRequest.respondedAt());
       return entity;
     }
   }
   ```

6. Update database schema:
   ```sql
   CREATE TABLE order_change_requests (
     id BIGINT AUTO_INCREMENT PRIMARY KEY,
     order_id VARCHAR(50) NOT NULL,
     items_to_add JSON NOT NULL,
     items_to_remove JSON NOT NULL,
     status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
     requested_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
     responded_at TIMESTAMP NULL,
     responded_by VARCHAR(50) NULL,
     FOREIGN KEY (order_id) REFERENCES orders(id) ON DELETE CASCADE,
     INDEX idx_order_id (order_id),
     INDEX idx_status (status)
   );
   ```

7. Create integration tests:
   
   - Test create and retrieve change request
   - Test update status to APPROVED and verify order update
   - Test update status to REJECTED and verify order unchanged
   - Test event published on approval
   - Test event published on rejection
   - Test transactional rollback on error
   - Test query after status update shows correct state
   
   ```java
   @Test
   void testUpdateStatusToApproved() {
     // Setup
     OrderChangeRequestEntity changeRequest = createChangeRequest(orderId, PENDING);
     
     // Execute
     orderChangeRequestRepositoryAdapter.updateChangeRequestStatus(
       changeRequest.toDomain(), 
       "APPROVED"
     );
     
     // Verify
     OrderChangeRequestEntity updated = jpaRepository.findById(changeRequest.getId()).orElseThrow();
     assert updated.getStatus().equals("APPROVED");
     assert updated.getRespondedAt() != null;
   }
   
   @Test
   void testUpdateStatusToRejected() {
     // Setup
     OrderChangeRequestEntity changeRequest = createChangeRequest(orderId, PENDING);
     
     // Execute
     orderChangeRequestRepositoryAdapter.updateChangeRequestStatus(
       changeRequest.toDomain(), 
       "REJECTED"
     );
     
     // Verify
     OrderChangeRequestEntity updated = jpaRepository.findById(changeRequest.getId()).orElseThrow();
     assert updated.getStatus().equals("REJECTED");
     assert updated.getRespondedAt() != null;
   }
   ```

8. Wire repositories and adapters in Spring configuration:
   - Register `OrderChangeRequestRepositoryAdapter` as bean
   - Inject into use cases via constructor

9. Event integration:
   - Events are published via existing `EventPublisherAdapter`
   - No additional persistence beyond audit log (already handled)

**Gherkin Scenarios**
Feature: Approve or Reject Order Changes Persistence

Scenario: Create and retrieve change request
  Given a change request for order "ord-001"
  And items to add and remove specified
  When the change request is saved to the database
  Then the request can be retrieved by order ID
  And status is "PENDING"

Scenario: Mark change request as approved
  Given a change request with status "PENDING"
  When updateChangeRequestStatus is called with "APPROVED"
  Then the database shows status = "APPROVED"
  And responded_at timestamp is set

Scenario: Mark change request as rejected
  Given a change request with status "PENDING"
  When updateChangeRequestStatus is called with "REJECTED"
  Then the database shows status = "REJECTED"
  And responded_at timestamp is set

Scenario: Approval also updates order
  Given a change request and order in same transaction
  When approval is processed
  Then change request status = "APPROVED"
  And order items are updated
  And order estimated_readiness_at is updated

Scenario: Rejection does not update order
  Given a change request and order in same transaction
  When rejection is processed
  Then change request status = "REJECTED"
  And order items remain unchanged
  And order estimated_readiness_at remains unchanged

Scenario: Event published on approval
  Given a change request is approved
  When the event is published
  Then OrderChangeApprovedEvent is persisted to events table

Scenario: Event published on rejection
  Given a change request is rejected
  When the event is published
  Then OrderChangeRejectedEvent is persisted to events table

Scenario: Transactional rollback on error
  Given a change request approval operation
  When a constraint is violated
  Then the transaction rolls back
  And status remains "PENDING"

Scenario: Query after approval returns latest state
  Given a change request is approved
  When queried immediately after
  Then status is "APPROVED" (not cached stale data)
  And responded_at is set

Scenario: Multiple change requests - approve one
  Given change requests "cr-001" and "cr-002" for different orders
  When "cr-001" is approved
  Then only "cr-001" status = "APPROVED"
  And "cr-002" status = "PENDING" (unchanged)

Scenario: Concurrent approvals from different bartenders on same change request
  Given a change request "cr-003" with status "PENDING"
  And bartender "b-001" and "b-002" both update it concurrently
  When both attempt updateChangeRequestStatus("cr-003", "APPROVED") simultaneously
  Then one update succeeds
  And the other either:
    - Succeeds idempotently (status already APPROVED), or
    - Throws OptimisticLockException (version conflict)
  And status is updated exactly once in database

Scenario: Verify prepared items count is persisted
  Given a change request with itemsToAdd: [beer, wine] and itemsToRemove: [water]
  When saved to database
  Then retrieved change request contains:
    - itemsToAdd JSON with 2 items
    - itemsToRemove JSON with 1 item
  And structure can be deserialized correctly

Scenario: Partial approval with item transfer tracking
  Given change request with 2 prepared items available for transfer
  When one is transferred and approval processed
  Then prepared_items_used field updated (if tracked)
  And order items correctly reflect transfer
  And responded_by field records bartender ID

Scenario: Constraint violation prevents invalid status transition
  Given a change request with status "REJECTED"
  When attempting to update it to "APPROVED"
  Then database constraint prevents transition (or domain logic)
  And status remains "REJECTED"
  And responded_at is NOT modified

**Notes**
- Change requests are tracked separately from orders for audit trail.
- JSON columns store item list for flexibility (alternative: separate join table).
- Status transitions: PENDING → (APPROVED or REJECTED), never back to PENDING.
- Responded_at and responded_by track bartender actions for audit.
- Events are published alongside state changes for notification.
- This feature builds on existing order persistence and adds change request tracking.
