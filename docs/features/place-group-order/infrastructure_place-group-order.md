# Infrastructure: Place a Group Order Persistence

**Context**
The Infrastructure module must provide persistent storage for group orders and manage the database schema for tracking multiple contributors and their individual token contributions. This includes storing contribution relationships and ensuring transactional consistency when reserving tokens across multiple festival goers.

**Problem**
The Infrastructure module requires JPA entity mapping, database schema, and repository implementation for group orders while maintaining clean separation and handling multi-contributor token reservation atomically.

**Acceptance Criteria**
- [ ] `GroupOrderEntity` JPA entity created:
  - Fields: `id`, `status`, `createdAt`, `updatedAt`
  - Mapped to database table `group_orders`
  - Relationship to `OrderEntity` (one-to-one inheritance or composition)
  
- [ ] `TokenContributionEntity` JPA entity created:
  - Fields: `id`, `groupOrderId`, `contributorId`, `drinkTokensContributed`, `snackTokensContributed`
  - Mapped to database table `token_contributions`
  - Foreign key to `group_orders`
  - Unique constraint: (groupOrderId, contributorId)
  
- [ ] `OrderItemEntity` updated if needed:
  - Can be shared between single and group orders
  
- [ ] `OrderRepositoryAdapter` extended:
  - Method: `saveGroupOrder(GroupOrder): OrderId`
  - Persists group order with all contributions atomically
  - Persists order items
  
- [ ] `GroupOrderRepositoryAdapter` (new) implements Domain port:
  - Method: `save(GroupOrder): OrderId`
  - Method: `findById(OrderId): Optional<GroupOrder>`
  - Loads group order with full contributor list and contributions
  
- [ ] `GroupOrderJpaRepository` Spring Data repository:
  - Extends `JpaRepository<GroupOrderEntity, String>`
  - Custom query: `findByOrderId(String)` (joins with contributions)
  
- [ ] `TokenContributionJpaRepository` Spring Data repository:
  - Extends `JpaRepository<TokenContributionEntity, Long>`
  - Custom query: `findByGroupOrderId(String): List<TokenContributionEntity>`
  
- [ ] `GroupOrderEntityMapper` converts between Domain and JPA:
  - Method: `toDomain(GroupOrderEntity, List<TokenContributionEntity>): GroupOrder`
  - Method: `toEntity(GroupOrder): Pair<GroupOrderEntity, List<TokenContributionEntity>>`
  
- [ ] `TokenContributionEntityMapper`:
  - Method: `toDomain(TokenContributionEntity): TokenContribution`
  - Method: `toEntity(TokenContribution, groupOrderId): TokenContributionEntity`
  
- [ ] Database migration script:
  - Creates `group_orders` table with proper schema
  - Creates `token_contributions` table with foreign key and unique constraint
  - Adds indexes for performance (groupOrderId, contributorId)
  - Ensures referential integrity
  
- [ ] Transactional consistency:
  - `saveGroupOrder` wrapped in `@Transactional`
  - All contributor balance updates happen in same transaction
  - Atomicity: all succeed or all fail
  
- [ ] Exception handling:
  - `GroupOrderNotFoundException`
  - `DataIntegrityViolationException` converted to domain exception
  
- [ ] Integration tests verify:
  - Group order persistence with all contributions
  - Retrieval of group order with contribution details
  - Transactional consistency (all or nothing)
  - Constraint violations caught

**Implementation Plan**
1. Create JPA entities in `persistence` package:
   
   - `TokenContributionEntity`:
     - `@Entity`
     - `@Table(name = "token_contributions", uniqueConstraints = @UniqueConstraint(columnNames = {"group_order_id", "contributor_id"}))`
     - `id: Long` (@Id @GeneratedValue)
     - `groupOrderId: String` (foreign key)
     - `contributorId: String`
     - `drinkTokensContributed: int`
     - `snackTokensContributed: int`
   
   - `GroupOrderEntity`:
     - `@Entity`
     - `@Table(name = "group_orders")`
     - `id: String` (@Id)
     - `status: String`
     - `createdAt: Instant`
     - `updatedAt: Instant`
     - `contributions: List<TokenContributionEntity>` (@OneToMany cascade=ALL)

2. Update `OrderEntity`:
   - Consider single-table inheritance or composition:
     - Option A: Add `isGroupOrder: boolean` flag to `OrderEntity`
     - Option B: Separate table with FK to `orders`
   - Implementation choice: Add `@DiscriminatorColumn(name = "order_type")` for single-table inheritance
   - `OrderType.INDIVIDUAL` vs `OrderType.GROUP`

3. Create Spring Data repositories in `persistence` package:
   
   - `GroupOrderJpaRepository`:
     - Extends `JpaRepository<GroupOrderEntity, String>`
     - `@Query` to load with contributions eagerly
   
   - `TokenContributionJpaRepository`:
     - Extends `JpaRepository<TokenContributionEntity, Long>`
     - Method: `findByGroupOrderId(String): List<TokenContributionEntity>`

4. Create `GroupOrderRepositoryAdapter` in `persistence` package:
   - Implements Domain port `GroupOrderRepository`
   - Inject: `GroupOrderJpaRepository`, `TokenContributionJpaRepository`, `GroupOrderEntityMapper`
   - Method: `save(GroupOrder): OrderId`
     - Create GroupOrderEntity
     - Create list of TokenContributionEntity from contributions
     - Save group order and contributions
     - Return OrderId
   - Method: `findById(OrderId): Optional<GroupOrder>`
     - Load GroupOrderEntity by ID
     - Load all TokenContributionEntity for this group order
     - Map to Domain GroupOrder
     - Return as Optional

5. Extend `OrderRepositoryAdapter`:
   - Method: `saveGroupOrder(GroupOrder): OrderId`
   - Delegates to `GroupOrderRepositoryAdapter.save()`
   - Transactional wrapper

6. Create mappers in `persistence/mapper` package:
   
   - `TokenContributionEntityMapper`:
     - Method: `toDomain(TokenContributionEntity): TokenContribution`
     - Method: `toEntity(TokenContribution, groupOrderId): TokenContributionEntity`
   
   - `GroupOrderEntityMapper`:
     - Method: `toDomain(GroupOrderEntity, List<TokenContributionEntity>): GroupOrder`
     - Reconstructs GroupOrder with contributions
     - Method: `toEntity(GroupOrder): Pair<GroupOrderEntity, List<TokenContributionEntity>>`
     - Creates entities from domain model

7. Create database migration:
   
   ```sql
   -- Update orders table for single-table inheritance
   ALTER TABLE orders ADD COLUMN order_type VARCHAR(10) NOT NULL DEFAULT 'INDIVIDUAL';
   
   -- Create group_orders table (if separate inheritance)
   CREATE TABLE group_orders (
     id VARCHAR(50) PRIMARY KEY,
     status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
     created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
     updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
     FOREIGN KEY (id) REFERENCES orders(id) ON DELETE CASCADE
   );
   
   -- Create token_contributions table
   CREATE TABLE token_contributions (
     id BIGINT AUTO_INCREMENT PRIMARY KEY,
     group_order_id VARCHAR(50) NOT NULL,
     contributor_id VARCHAR(50) NOT NULL,
     drink_tokens_contributed INT NOT NULL,
     snack_tokens_contributed INT NOT NULL,
     UNIQUE KEY uk_group_order_contributor (group_order_id, contributor_id),
     FOREIGN KEY (group_order_id) REFERENCES group_orders(id) ON DELETE CASCADE,
     FOREIGN KEY (contributor_id) REFERENCES festival_goers(id)
   );
   
   CREATE INDEX idx_token_contributions_group_order ON token_contributions(group_order_id);
   CREATE INDEX idx_token_contributions_contributor ON token_contributions(contributor_id);
   ```

8. Create integration tests:
   - Test save and retrieve group order with 2 contributors
   - Test save and retrieve with 3+ contributors
   - Test retrieve all contributions for a group order
   - Test transactional rollback on constraint violation
   - Test cascade delete (delete group order → delete contributions)
   - Test unique constraint on (groupOrderId, contributorId)
   - Test foreign key constraint enforcement

**Gherkin Scenarios**
Feature: Group Order Persistence

Scenario: Save and retrieve group order with 2 contributors
  Given a GroupOrder for order ID "ord-001"
  And contributor "fgv-001" contributing 1 drink token
  And contributor "fgv-002" contributing 1 drink token
  When the group order is saved to the database
  Then the group order can be retrieved by ID
  And the group order contains 2 contributions
  And "fgv-001" is associated with 1 drink token contributed
  And "fgv-002" is associated with 1 drink token contributed

Scenario: Save and retrieve group order with 3+ contributors
  Given a GroupOrder for order ID "ord-002"
  And contributors: "fgv-001" (2 tokens), "fgv-002" (2 tokens), "fgv-003" (1 token)
  When the group order is saved to the database
  Then the group order can be retrieved
  And all 3 contributions are persisted with correct amounts

Scenario: Save group order with mixed token types
  Given a GroupOrder for order ID "ord-003"
  And contributor "fgv-001" contributing 1 drink token, 2 snack tokens
  And contributor "fgv-002" contributing 1 drink token, 3 snack tokens
  When the group order is saved to the database
  And the group order is retrieved
  Then contributions show correct breakdown per token type

Scenario: Group order not found in database
  Given no group order with ID "ord-999" exists
  When retrieving group order by ID "ord-999"
  Then no group order is returned (Optional.empty())

Scenario: Update group order status
  Given a GroupOrder with status "PENDING"
  And the group order is saved to the database
  When the status is changed to "ACKNOWLEDGED"
  And the group order is persisted
  Then retrieving the group order shows updated status

Scenario: Cascade delete removes all contributions
  Given a GroupOrder with 3 contributions
  And the group order is saved to the database
  When the group order is deleted from the database
  Then the group order is no longer retrievable
  And all 3 contribution records are also deleted

Scenario: Unique constraint prevents duplicate contributor
  Given an attempt to add 2 contributions for same contributor to same group order
  When saving to the database
  Then a unique constraint violation exception is raised

Scenario: Foreign key constraint enforces valid contributor
  Given an attempt to create a contribution with non-existent contributorId
  When saving to the database
  Then a foreign key constraint violation exception is raised

Scenario: Timestamps are tracked automatically
  Given a GroupOrder is saved at time T1
  When the group order is retrieved
  Then `createdAt` is approximately T1
  And `updatedAt` is approximately T1
  When status is updated at time T2
  And the group order is saved and retrieved
  Then `createdAt` remains unchanged
  And `updatedAt` is approximately T2

Scenario: Retrieve all contributions for a specific group order
  Given group orders "ord-001", "ord-002", "ord-003" exist
  And "ord-001" has 2 contributions
  And "ord-002" has 3 contributions
  When retrieving all contributions for "ord-001"
  Then exactly 2 contributions are returned
  And they all have groupOrderId = "ord-001"

**Notes**
- This feature provides Infrastructure persistence for "Place a Group Order" (feature 5).
- JPA entities contain framework-specific annotations (allowed here).
- Domain models are never persisted directly; only JPA entities touch the database.
- Single-table inheritance for Order/GroupOrder allows unified queries on orders table.
- Token contributions are modeled as a dependent relationship (OneToMany with cascade delete).
- Unique constraint on (groupOrderId, contributorId) prevents duplicate contributions.
- Transactional wrapper ensures atomicity: all contributor balances reserved or none reserved.
- Indexes on commonly queried columns improve performance.
