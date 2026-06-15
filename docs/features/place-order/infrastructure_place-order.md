# Infrastructure: Place an Order Persistence

**Context**
The Infrastructure module must provide persistent storage for orders and manage database schema. This unified feature covers:
- Storing orders with multiple items (drinks, snacks, meals)
- Managing order state transitions
- Retrieving orders by ID or festival goer ID
- Updating token balances when orders are placed

**Problem**
The Infrastructure module requires JPA entity mapping, database schema, and repository implementation for the `OrderRepository` port defined in the Domain, while maintaining clean separation of concerns.

**Acceptance Criteria**
- [ ] `OrderEntity` JPA entity created:
  - Fields: `id`, `festivalGoerId`, `status`, `createdAt`, `updatedAt`
  - Mapped to database table `orders`
  - Proper column types and constraints
  
- [ ] `OrderItemEntity` JPA embeddable/entity created:
  - Fields: `itemType` (DRINK/FOOD), `itemSubtype` (enum), `quantity`, `costInTokens`
  - Proper JPA mapping (OneToMany relationship with Order)
  - Mapped to database table `order_items`
  
- [ ] `OrderRepositoryAdapter` implements Domain's `OrderRepository` port:
  - Method: `save(Order): OrderId`
  - Method: `findById(OrderId): Optional<Order>`
  - Method: `findByFestivalGoerId(FestivalGoerId): List<Order>`
  
- [ ] `OrderJpaRepository` Spring Data repository created:
  - Extends `JpaRepository<OrderEntity, String>`
  - Custom query: `findByFestivalGoerId(String)`
  
- [ ] `OrderEntityMapper` converts between Domain and JPA:
  - Method: `toDomain(OrderEntity): Order`
  - Method: `toEntity(Order): OrderEntity`
  - Handles list of order items properly
  
- [ ] `OrderItemEntityMapper`:
  - Method: `toDomain(OrderItemEntity): OrderItem`
  - Method: `toEntity(OrderItem): OrderItemEntity`
  
- [ ] Database migration script:
  - Creates `orders` table with proper schema
  - Creates `order_items` table with foreign key to orders
  - Adds indexes for performance (festivalGoerId, status, createdAt)
  - Ensures referential integrity
  
- [ ] Exception handling:
  - `OrderNotFoundException` when order not found
  - Proper constraint violation handling
  
- [ ] Integration tests verify:
  - Order persistence and retrieval
  - Order item relationships
  - Query by ID
  - Query by festival goer ID
  - Order state updates
  - Timestamp tracking

**Implementation Plan**
1. Create JPA entities in `persistence` package:
   
   - `OrderItemEntity`:
     - `@Embeddable` or `@Entity`
     - Fields: `itemType: String` (DRINK/FOOD)
     - `itemSubtype: String` (NON_ALCOHOLIC, NORMAL_ALCOHOLIC, PREMIUM_ALCOHOLIC, SNACK, MEAL)
     - `quantity: int`
     - `costInTokens: int`
   
   - `OrderEntity` (main entity):
     - `@Entity`
     - `@Table(name = "orders")`
     - `id: String` (@Id)
     - `festivalGoerId: String`
     - `status: String` (PENDING, ACKNOWLEDGED, READY, CANCELLED)
     - `items: List<OrderItemEntity>` (@OneToMany cascade)
     - `createdAt: Instant` (@CreationTimestamp)
     - `updatedAt: Instant` (@UpdateTimestamp)

2. Create `OrderJpaRepository` interface:
   - Extends `JpaRepository<OrderEntity, String>`
   - Method: `findByFestivalGoerId(String): List<OrderEntity>`
   - Optional index hints for performance

3. Create `OrderRepositoryAdapter` in `persistence` package:
   - Implements Domain port `OrderRepository`
   - Inject: `OrderJpaRepository`, `OrderEntityMapper`
   - Method: `save(Order): OrderId`
     - Map Domain Order to OrderEntity
     - Save via JpaRepository
     - Return OrderId
   - Method: `findById(OrderId): Optional<Order>`
     - Load OrderEntity by ID
     - Map to Domain Order if found
     - Return as Optional
   - Method: `findByFestivalGoerId(FestivalGoerId): List<Order>`
     - Query by festivalGoerId
     - Map all to Domain Orders
     - Return list

4. Create mappers in `persistence/mapper` package:

   - `OrderItemEntityMapper`:
     - Method: `toDomain(OrderItemEntity): OrderItem`
     - Method: `toEntity(OrderItem): OrderItemEntity`
     - Handle subtype enum conversions
   
   - `OrderEntityMapper`:
     - Method: `toDomain(OrderEntity): Order`
     - Method: `toEntity(Order): OrderEntity`
     - Delegate item mapping to `OrderItemEntityMapper`

5. Create database migration (Flyway or Liquibase):
   
   ```sql
   CREATE TABLE orders (
     id VARCHAR(50) PRIMARY KEY,
     festival_goer_id VARCHAR(50) NOT NULL,
     status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
     created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
     updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
     FOREIGN KEY (festival_goer_id) REFERENCES festival_goers(id)
   );
   
   CREATE INDEX idx_orders_festival_goer_id ON orders(festival_goer_id);
   CREATE INDEX idx_orders_status ON orders(status);
   CREATE INDEX idx_orders_created_at ON orders(created_at);
   
   CREATE TABLE order_items (
     id BIGINT AUTO_INCREMENT PRIMARY KEY,
     order_id VARCHAR(50) NOT NULL,
     item_type VARCHAR(10) NOT NULL,
     item_subtype VARCHAR(30) NOT NULL,
     quantity INT NOT NULL,
     cost_in_tokens INT NOT NULL,
     FOREIGN KEY (order_id) REFERENCES orders(id) ON DELETE CASCADE
   );
   
   CREATE INDEX idx_order_items_order_id ON order_items(order_id);
   ```

6. Create integration tests:
   - Test save and retrieve single order
   - Test save and retrieve order with multiple items (mixed drinks and food)
   - Test retrieve order by ID (found/not found)
   - Test retrieve all orders for a festival goer
   - Test update order status to ACKNOWLEDGED, READY, CANCELLED
   - Test timestamp auto-updates
   - Test cascade delete (delete order → delete order items)
   - Test concurrent order placement by different festival goers
   - Test query performance with index coverage

**Gherkin Scenarios**
Feature: Order Persistence

Scenario: Save and retrieve order with single drink item
  Given an Order for festival goer "fgv-001"
  And the order contains 1 normal alcoholic drink
  When the order is saved to the database
  Then the order can be retrieved by ID
  And the order contains 1 item
  And the item cost is 1 drink token
  And the order status is "PENDING"

Scenario: Save and retrieve order with multiple drink items
  Given an Order for festival goer "fgv-001"
  And the order contains 2 normal alcoholic drinks and 1 non-alcoholic drink
  When the order is saved to the database
  Then the order can be retrieved by ID
  And the order contains 3 items
  And the order cost is 2 drink tokens
  And items are in the correct order

Scenario: Save and retrieve order with food items
  Given an Order for festival goer "fgv-001"
  And the order contains 2 snacks and 1 meal
  When the order is saved to the database
  Then the order can be retrieved by ID
  And the order contains 3 items
  And the order cost is 5 snack tokens

Scenario: Save and retrieve complex mixed order
  Given an Order for festival goer "fgv-001"
  And the order contains 2 normal alcoholic drinks, 1 non-alcoholic drink, 2 snacks, and 1 meal
  When the order is saved to the database
  Then the order can be retrieved by ID
  And the order contains 6 items
  And the order drink token cost is 2
  And the order snack token cost is 5

Scenario: Retrieve all orders for a festival goer
  Given festival goer "fgv-001" has 3 orders:
    | OrderId | Status | Items |
    | ord-001 | PENDING | 1 drink |
    | ord-002 | ACKNOWLEDGED | 2 snacks |
    | ord-003 | READY | 1 meal |
  When retrieving all orders for "fgv-001"
  Then 3 orders are returned
  And the orders are in creation order

Scenario: Retrieve non-existent order returns empty
  Given no order with ID "ord-999" exists
  When retrieving order by ID "ord-999"
  Then no order is returned (Optional.empty())

Scenario: Update order status to ACKNOWLEDGED
  Given an Order for festival goer "fgv-001" with status "PENDING"
  And the order is saved to the database
  When the order status is changed to "ACKNOWLEDGED"
  And the order is saved
  Then the order can be retrieved
  And the retrieved order has status "ACKNOWLEDGED"

Scenario: Update order status to READY
  Given an Order for festival goer "fgv-001" with status "ACKNOWLEDGED"
  And the order is saved to the database
  When the order status is changed to "READY"
  And the order is saved
  Then the retrieved order has status "READY"

Scenario: Update order status to CANCELLED
  Given an Order for festival goer "fgv-001" with status "PENDING"
  And the order is saved to the database
  When the order status is changed to "CANCELLED"
  And the order is saved
  Then the retrieved order has status "CANCELLED"

Scenario: Timestamps are tracked automatically
  Given an Order is saved at time T1
  When the order is retrieved
  Then the `createdAt` timestamp is set to approximately T1
  And the `updatedAt` timestamp is set to approximately T1
  When the order is updated at time T2
  And the order is saved
  And the order is retrieved
  Then the `createdAt` timestamp remains unchanged
  And the `updatedAt` timestamp is updated to approximately T2

Scenario: Cascade delete removes order items
  Given an Order for festival goer "fgv-001"
  And the order contains 3 items
  And the order is saved to the database
  When the order is deleted from the database
  Then the order is no longer retrievable
  And all 3 order items are also deleted

Scenario: Database constraint enforces quantity > 0
  Given an attempt to create an OrderItemEntity with quantity 0
  When the entity is persisted
  Then a database constraint violation exception is raised

Scenario: Foreign key constraint enforces valid festival goer
  Given an attempt to create an OrderEntity with non-existent festivalGoerId
  When the entity is persisted
  Then a foreign key constraint violation exception is raised

**Notes**
- This feature provides Infrastructure persistence for the unified "Place an Order" feature (covers features 2, 3, 4).
- JPA entities contain framework-specific annotations (allowed here).
- Domain models are never persisted directly; only JPA entities touch the database.
- The `OrderRepositoryAdapter` bridges Domain ports and Infrastructure JPA repositories.
- All mapping occurs in the mapper layer to keep persistence agnostic of Domain concerns.
- Order items are modeled as a dependent relationship (OneToMany with cascade delete).
- Indexes on commonly queried columns (festivalGoerId, status) improve performance.
- Status field is denormalized (could use enum JPA converter) for query flexibility.
