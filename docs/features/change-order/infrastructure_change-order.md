# Infrastructure: Change an Order Persistence

**Context**
The Infrastructure module must persist order changes to the database while ensuring token balance updates are transactional. When an order is modified with new items added or removed, the associated OrderItem entries and TokenBalance reserved amounts must be updated consistently. The update operation must be atomic: either all changes succeed or all fail.

**Problem**
The Infrastructure layer requires repository implementations to handle order updates with cascading item modifications and coordinated token balance adjustments, while maintaining data consistency through transactional boundaries.

**Acceptance Criteria**
- [ ] `OrderEntity` JPA entity updated:
  - Field: `updatedAt: Instant` (already present from place-order, ensure not null on update)
  - Relationship: `items: List<OrderItemEntity>` with cascade=ALL
  - Ensure cascade delete works for removed items
  
- [ ] `OrderItemEntity` JPA entity (if not already present):
  - Fields: `id`, `orderId`, `menuItemId`, `quantity`, `itemType`
  - Relationship: cascade delete when item is removed from order
  
- [ ] `OrderRepositoryAdapter` extended:
  - Method: `updateOrder(Order): void` (or update existing `saveOrder` to handle both insert and update)
  - Persist order with cascading item updates
  - Update `updatedAt` timestamp automatically
  - Handle item additions (new OrderItemEntity records)
  - Handle item removals (delete OrderItemEntity records via cascade)
  
- [ ] `OrderJpaRepository` Spring Data repository:
  - Method: `findById(String): Optional<OrderEntity>` (likely already exists)
  - Ensure eager loading of items: `@Query` with join fetch if needed
  
- [ ] `OrderEntityMapper` extended:
  - Method: `toDomain(OrderEntity): Order` (existing)
  - Method: `toEntity(Order): OrderEntity` (existing or new)
  - Ensure cascading items are correctly mapped
  
- [ ] `OrderItemEntityMapper`:
  - Method: `toDomain(OrderItemEntity): OrderItem`
  - Method: `toEntity(OrderItem): OrderItemEntity`
  
- [ ] Update transactional wrapper in `OrderRepositoryAdapter`:
  - All order updates wrapped in `@Transactional`
  - Atomicity: all items persisted or none persisted
  
- [ ] Exception handling:
  - `DataIntegrityViolationException` caught and converted to domain exception if needed
  - `EntityNotFoundException` if order not found during update
  
- [ ] Database operations:
  - Update `orders.updated_at` timestamp on modification
  - Insert new `order_items` rows for added items
  - Delete `order_items` rows for removed items (cascade)
  - Handle orphan removal: cascade delete of items no longer in the order list
  
- [ ] Integration tests verify:
  - Order items are updated (added items persisted)
  - Order items are removed (deleted from database)
  - `updated_at` timestamp is refreshed
  - Transactional rollback on constraint violation
  - Cascade delete removes associated items
  - Querying order after update shows correct item list
  - Token balance repository calls are coordinated correctly

**Implementation Plan**
1. Update `OrderEntity` JPA entity in `persistence` package (if not already present):
   - `@Entity`
   - `@Table(name = "orders")`
   - `id: String` (@Id)
   - `festivalGoerId: String` (FK to festival_goers)
   - `status: String` (OrderStatus enum or string)
   - `createdAt: Instant` (@Temporal or @Column with temporal type)
   - `updatedAt: Instant` (@PreUpdate to auto-update, or explicit set)
   - `items: List<OrderItemEntity>` (@OneToMany(cascade=ALL, orphanRemoval=true))

2. Create/Update `OrderItemEntity` JPA entity:
   - `@Entity`
   - `@Table(name = "order_items")`
   - `id: Long or String` (@Id @GeneratedValue)
   - `orderId: String` (FK to orders)
   - `menuItemId: String`
   - `quantity: int`
   - `itemType: String` (DRINK or FOOD)
   - `order: OrderEntity` (@ManyToOne)

3. Create/Extend `OrderJpaRepository` Spring Data repository:
   - `extends JpaRepository<OrderEntity, String>`
   - Ensure items are eagerly loaded:
     ```java
     @Query("SELECT o FROM OrderEntity o LEFT JOIN FETCH o.items WHERE o.id = :id")
     Optional<OrderEntity> findById(@Param("id") String id);
     ```

4. Create `OrderRepositoryAdapter` in `persistence` package:
   - Inject: `OrderJpaRepository`, `OrderEntityMapper`, `TokenBalanceRepository`
   - Method: `findOrderById(String): Optional<Order>`
     - Load OrderEntity with items
     - Map to Domain Order via mapper
     - Return as Optional
   
   - Method: `saveOrder(Order): void` (update or insert)
     ```java
     @Transactional
     public void saveOrder(Order order) {
       OrderEntity entity = orderEntityMapper.toEntity(order);
       orderJpaRepository.save(entity); // Works for both insert and update
     }
     ```
   - Set updatedAt before save if it's an update

5. Create mappers in `persistence/mapper` package:
   
   - `OrderItemEntityMapper`:
     ```java
     public OrderItem toDomain(OrderItemEntity entity) {
       return new OrderItem(entity.getId(), entity.getMenuItemId(), 
                            entity.getQuantity(), ItemType.valueOf(entity.getItemType()));
     }
     
     public OrderItemEntity toEntity(OrderItem item) {
       OrderItemEntity entity = new OrderItemEntity();
       entity.setId(item.getId());
       entity.setMenuItemId(item.getMenuItemId());
       entity.setQuantity(item.getQuantity());
       entity.setItemType(item.getItemType().toString());
       return entity;
     }
     ```
   
   - `OrderEntityMapper` (extend if exists):
     ```java
     public Order toDomain(OrderEntity entity) {
       List<OrderItem> items = entity.getItems().stream()
         .map(orderItemEntityMapper::toDomain)
         .toList();
       return new Order(entity.getId(), entity.getFestivalGoerId(), 
                        items, OrderStatus.valueOf(entity.getStatus()));
     }
     
     public OrderEntity toEntity(Order order) {
       OrderEntity entity = new OrderEntity();
       entity.setId(order.getId());
       entity.setFestivalGoerId(order.getFestivalGoerId());
       entity.setStatus(order.getStatus().toString());
       entity.setUpdatedAt(Instant.now()); // Auto-set timestamp on update
       List<OrderItemEntity> itemEntities = order.getItems().stream()
         .map(orderItemEntityMapper::toEntity)
         .toList();
       entity.setItems(itemEntities);
       itemEntities.forEach(item -> item.setOrder(entity)); // Bidirectional
       return entity;
     }
     ```

6. Update database schema (if using migrations):
   
   ```sql
   -- Add updated_at to orders table if not present
   ALTER TABLE orders ADD COLUMN updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP;
   
   -- Create order_items table if not present
   CREATE TABLE order_items (
     id BIGINT AUTO_INCREMENT PRIMARY KEY,
     order_id VARCHAR(50) NOT NULL,
     menu_item_id VARCHAR(50) NOT NULL,
     quantity INT NOT NULL,
     item_type VARCHAR(10) NOT NULL, -- DRINK or FOOD
     FOREIGN KEY (order_id) REFERENCES orders(id) ON DELETE CASCADE
   );
   
   CREATE INDEX idx_order_items_order_id ON order_items(order_id);
   
   -- Add trigger to auto-update updated_at
   CREATE TRIGGER trg_orders_updated_at
   BEFORE UPDATE ON orders
   FOR EACH ROW
   SET NEW.updated_at = CURRENT_TIMESTAMP;
   ```

7. Create integration tests:
   - Test save new order with items
   - Test add items to existing order
   - Test remove items from existing order
   - Test replace items (remove old, add new)
   - Test updatedAt is changed on update
   - Test retrieve order with all items after update
   - Test cascade delete removes items when order is deleted
   - Test transactional rollback on constraint violation
   - Test orphan removal (cascade delete of orphaned items)

8. Ensure transactional consistency:
   - Wrap repository operations in `@Transactional`
   - Token balance and order updates should be in same transaction (at service level)
   - All or nothing: if token balance save fails, order update fails too

**Gherkin Scenarios**
Feature: Order Update Persistence

Scenario: Add item to order and persist
  Given an OrderEntity with ID "ord-001" and 1 existing item
  When an OrderItem is added to the order
  And the order is saved to the database
  Then the database contains 2 order_items records for "ord-001"
  And the updated_at timestamp is refreshed

Scenario: Remove item from order and persist
  Given an OrderEntity with ID "ord-002" and 2 items (item-1, item-2)
  When item-1 is removed from the order
  And the order is saved to the database
  Then the database contains 1 order_items record for "ord-002"
  And item-1 is no longer in the order_items table
  And cascade delete occurs

Scenario: Replace items (remove + add)
  Given an OrderEntity with ID "ord-003" with 1 item (non-alcoholic drink)
  When the item is removed
  And a new item (premium drink) is added
  And the order is saved
  Then the database reflects only the new item for "ord-003"
  And updated_at is changed

Scenario: Retrieve order with all updated items
  Given an order "ord-004" was updated with new items
  When the order is retrieved from the database by ID
  Then all current items are loaded (not stale items)
  And the item count matches the current state

Scenario: Multiple items update (add 2, remove 1)
  Given an order "ord-005" with 3 items
  When 2 new items are added
  And 1 old item is removed
  And the order is saved
  Then the database contains 4 items for "ord-005"

Scenario: Updated timestamp auto-refreshes
  Given an order "ord-006" was saved at time T1
  When the order is retrieved at time T1
  And updated_at is approximately T1
  When the order is updated at time T2
  And the order is persisted
  And retrieved at time T2
  Then updated_at is approximately T2 (not T1)
  And createdAt remains unchanged

Scenario: Cascade delete removes all items when order deleted
  Given an order "ord-007" with 3 items
  And the order is saved to the database
  When the order is deleted from the database
  Then the order record is deleted
  And all 3 order_items records are also deleted (cascade)

Scenario: Orphan removal (item not in list anymore)
  Given an OrderEntity with item-1, item-2, item-3
  When item-2 is removed from the list
  And the order is saved with orphanRemoval=true
  Then item-2 is deleted from the database (orphan removed)

Scenario: Transactional rollback on constraint violation
  Given an order update operation
  When a constraint violation occurs (e.g., menu item not found)
  Then the transaction rolls back
  And the order state in the database is unchanged

Scenario: Order not found during update
  Given an attempt to update order "ord-999" (does not exist)
  When the order is retrieved
  Then Optional.empty() is returned
  And no update occurs

Scenario: Query after update returns latest data
  Given an order "ord-010" is updated in-memory
  And persisted to the database
  When the order is queried again immediately after
  Then the query result contains the updated items (not cached stale data)

**Notes**
- `OrderItemEntity` is persisted with a FK to `orders`, enabling cascade operations.
- `orphanRemoval=true` on `@OneToMany` ensures deleted items are cascade-deleted.
- The trigger (or JPA @PreUpdate) auto-updates `updated_at` without explicit code.
- Queries use FETCH JOIN to eagerly load items, preventing N+1 problems.
- Transactional boundaries are managed at the repository or service level.
- Token balance updates are handled separately in `TokenBalanceRepository` but should be in the same transaction (coordinated by the application service or controller).
- This feature builds on existing order persistence (from place-order feature) and adds update capability.
