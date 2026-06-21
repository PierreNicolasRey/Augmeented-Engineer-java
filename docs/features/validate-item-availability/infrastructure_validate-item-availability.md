# Infrastructure: Validate Item Availability for Orders

**Context**
The Infrastructure layer implements persistence for item inventory and provides the concrete implementation of the ItemInventoryRepository port. It also handles the wiring of dependencies and may provide test data setup for inventory.

**Problem**
The Infrastructure module must:
1. Persist ItemInventory entities to the database with transactional consistency
2. Implement the ItemInventoryRepository port efficiently
3. Ensure reservation state is correctly tracked and updated
4. Provide database schema for item inventory
5. Handle concurrent reservations safely (optimistic/pessimistic locking if needed)

**Acceptance Criteria**
- [ ] JPA Entity `ItemInventoryEntity` created:
  - `id: UUID` (primary key)
  - `itemId: String` (business identifier, unique)
  - `totalQuantity: int`
  - `reservedQuantity: int`
  - Mapped to table `item_inventories`
  - Version field for optimistic locking (for concurrent reservation safety)

- [ ] Mapper `ItemInventoryMapper` created:
  - `toDomain(ItemInventoryEntity): ItemInventory`
  - `toEntity(ItemInventory): ItemInventoryEntity`

- [ ] `ItemInventoryRepositoryAdapter` created:
  - Implements `ItemInventoryRepository` port
  - Uses Spring Data `ItemInventoryJpaRepository` for CRUD
  - `findByItemId(ItemId)`: queries by itemId, throws ItemNotFoundInCatalogException if not found
  - `findAllByItemIds(List<ItemId>)`: batch queries items
  - `save(ItemInventory)`: persists updated inventory
  - `saveAll(List<ItemInventory>)`: batch persists
  - Handles transaction demarcation

- [ ] Spring Data interface `ItemInventoryJpaRepository`:
  - Extends JpaRepository<ItemInventoryEntity, UUID>
  - Custom query: `findByItemId(String itemId)`

- [ ] Database migration (Flyway/Liquibase):
  - Creates `item_inventories` table
  - Columns: id (PK), item_id (UK), total_quantity, reserved_quantity, version, created_at, updated_at
  - Indexes on `item_id` for efficient lookups

- [ ] Configuration bean:
  - Wire `ItemInventoryRepositoryAdapter` as Spring bean implementing `ItemInventoryRepository`
  - Wire `ItemAvailabilityService` as Spring bean with injected repository
  - Export Service to Application layer

- [ ] Test fixtures / seed data:
  - Sample items in test database (mojito, beer, champagne, etc.)
  - Utilities for creating/resetting item inventories in tests

- [ ] Concurrency handling:
  - Use optimistic locking (version field) on ItemInventoryEntity
  - Catch OptimisticLockException and retry or propagate as business exception
  - Document retry strategy if needed

- [ ] Integration tests covering:
  - Persist and retrieve ItemInventory
  - Concurrent reservation attempts (if applicable)
  - Batch operations (findAllByItemIds, saveAll)
  - Foreign key/constraint validation

**Implementation Plan**
1. Create JPA Entity in `infrastructure/persistence/entity/` package:
   - `ItemInventoryEntity` with all fields and ORM annotations
   - Include `@Version` for optimistic locking

2. Create JPA Repository in `infrastructure/persistence/repository/` package:
   - `ItemInventoryJpaRepository` interface
   - Custom @Query for `findByItemId(String itemId)`

3. Create Mapper in `infrastructure/persistence/mapper/` package:
   - `ItemInventoryMapper` with bidirectional mapping logic

4. Create Repository Adapter in `infrastructure/persistence/` package:
   - `ItemInventoryRepositoryAdapter` implementing Domain port
   - Dependency inject: `ItemInventoryJpaRepository`
   - Translate exceptions appropriately

5. Create Spring configuration in `infrastructure/config/` package:
   - Bean definitions for `ItemInventoryRepositoryAdapter` and `ItemAvailabilityService`
   - Ensure Application layer can inject `ItemAvailabilityService`

6. Create database migration in `resources/db/migration/`:
   - `V{X}__Create_item_inventories_table.sql` (or .yaml for Liquibase)
   - Define schema as described

7. Create test fixtures in `infrastructure/test/fixtures/`:
   - Utility class for creating/seeding test inventories

8. Integration tests in `infrastructure/test/java/`:
   - Persistence tests
   - Concurrency tests (if applicable)
   - Batch operation tests

**Database Schema**
```sql
CREATE TABLE item_inventories (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  item_id VARCHAR(255) UNIQUE NOT NULL,
  total_quantity INTEGER NOT NULL,
  reserved_quantity INTEGER NOT NULL,
  version INTEGER DEFAULT 0,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT check_reserved_le_total CHECK (reserved_quantity <= total_quantity),
  CONSTRAINT check_quantities_non_negative CHECK (total_quantity >= 0 AND reserved_quantity >= 0)
);

CREATE INDEX idx_item_inventories_item_id ON item_inventories(item_id);
```

**Notes**
- Optimistic locking via `@Version` prevents race conditions on concurrent reservations
- If concurrent reservations are frequent and contentious, consider read-write locks at application level
- Test data setup should populate commonly-used items (mojito, beer, champagne, snacks, etc.)
