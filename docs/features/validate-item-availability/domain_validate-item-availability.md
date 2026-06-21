# Domain: Validate Item Availability

**Context**
When a festival goer places or modifies an order, the system must ensure that all requested items are available in sufficient quantities. Item stock follows a reservation model similar to tokens: items are reserved when an order is created/modified, and the physical stock is only decremented when the order is acknowledged.

**Problem**
The Domain module requires:
1. Validation that all requested items exist in the catalog
2. Validation that available (unreserved) stock is sufficient for each item
3. Reservation of stock when an order is placed or modified
4. Tracking of reserved vs. available stock per item
5. Handling of insufficient inventory scenarios

**Acceptance Criteria**
- [ ] `ItemId` Value Object created as immutable identifier
- [ ] `ItemInventory` Entity created with:
  - `itemId: ItemId`
  - `totalQuantity: int` (total stock)
  - `reservedQuantity: int` (reserved by pending/acknowledged orders)
  - Method: `getAvailableQuantity(): int` (calculated: totalQuantity - reservedQuantity)
  - Method: `canReserve(int quantity): boolean` (checks if available >= requested)
  - Method: `reserve(int quantity): void` (throws if insufficient, increments reservedQuantity)
  - Method: `releaseReservation(int quantity): void` (decrements reservedQuantity)
  - Invariant: `reservedQuantity <= totalQuantity` always holds

- [ ] `ItemNotFoundInCatalogException` created (thrown when item doesn't exist)
- [ ] `InsufficientItemInventoryException` created with:
  - `itemId`
  - `requestedQuantity`
  - `availableQuantity`
  - (thrown when reserved stock insufficient)

- [ ] `ItemInventoryRepository` Port created with methods:
  - `findByItemId(ItemId): ItemInventory` (or throw ItemNotFoundInCatalogException)
  - `findAllByItemIds(List<ItemId>): List<ItemInventory>`
  - `save(ItemInventory): void`
  - `saveAll(List<ItemInventory>): void`

- [ ] Domain Service `ItemAvailabilityService` (NOT a Use Case, just business logic):
  - Method: `validateAndReserveItems(List<OrderItem>): void`
    - For each item:
      - Load ItemInventory via repository
      - Throw ItemNotFoundInCatalogException if not found
      - Throw InsufficientItemInventoryException if canReserve fails
    - If all valid: reserve quantities and persist updated inventories
  - This service is called at the **beginning** of PlaceOrderUseCase and PlaceGroupOrderUseCase

- [ ] Stock is decremented (permanent consumption) when order is ACKNOWLEDGED (separate feature)

- [ ] Full unit test coverage:
  - Happy path: items available, reservations succeed
  - Item not found in catalog
  - Single item insufficient stock
  - Multiple items, one insufficient
  - Multiple items all insufficient
  - Reservation state verification

**Implementation Plan**
1. Create `ItemId` Value Object in `model` package:
   - Immutable identifier
   - Equals/hashCode implementation

2. Create `ItemInventory` Entity in `model` package:
   - Fields: `itemId: ItemId`, `totalQuantity: int`, `reservedQuantity: int`
   - Constructor validates quantities >= 0 and reservedQuantity <= totalQuantity
   - Methods as described above

3. Define `ItemInventoryRepository` Port in `ports` package:
   - Interface with methods as described

4. Create `ItemAvailabilityService` in `services` package:
   - Inject: `ItemInventoryRepository`
   - Method: `validateAndReserveItems(List<OrderItem>): void`
   - Handles full validation + reservation flow

5. Create exception classes in `exceptions` package:
   - `ItemNotFoundInCatalogException`
   - `InsufficientItemInventoryException`

6. Update `PlaceOrderUseCase`:
   - Call `itemAvailabilityService.validateAndReserveItems(items)` **before** creating the order
   - If validation fails, nothing is persisted (fail-fast)

7. Update `PlaceGroupOrderUseCase`:
   - Call `itemAvailabilityService.validateAndReserveItems(items)` **before** creating the group order
   - Same fail-fast behavior

8. Comprehensive unit tests in domain module

**Gherkin Scenarios**
Feature: Validate Item Availability for Orders

  Scenario: Order confirmed when items are in stock
    Given the following items are available in inventory:
      | itemId | totalQuantity | reservedQuantity |
      | mojito | 10            | 0                |
    When attempting to place an order for 2 "mojito"
    Then an Order is created with status PENDING
    And the "mojito" inventory is updated:
      | totalQuantity | reservedQuantity | availableQuantity |
      | 10            | 2                | 8                 |

  Scenario: Order rejected when item stock is insufficient
    Given the following items are available in inventory:
      | itemId | totalQuantity | reservedQuantity |
      | mojito | 1             | 0                |
    When attempting to place an order for 2 "mojito"
    Then the order creation is rejected
    And an InsufficientItemInventoryException is raised
    And the "mojito" inventory remains unchanged:
      | totalQuantity | reservedQuantity | availableQuantity |
      | 1             | 0                | 1                 |
    And no Order is created

  Scenario: Order rejected if item does not exist in catalog
    Given an empty item catalog
    When attempting to place an order for 1 "champagne"
    Then the order creation is rejected
    And an ItemNotFoundInCatalogException is raised
    And no Order is created

  Scenario: Order rejected when multiple items, one has insufficient stock
    Given the following items are available in inventory:
      | itemId      | totalQuantity | reservedQuantity |
      | mojito      | 5             | 0                |
      | beer        | 0             | 0                |
    When attempting to place an order for:
      | itemId | quantity |
      | mojito | 3        |
      | beer   | 2        |
    Then the order creation is rejected
    And an InsufficientItemInventoryException is raised for "beer"
    And both inventories remain unchanged
    And no Order is created

  Scenario: Order rejected when multiple items, all have insufficient stock
    Given the following items are available in inventory:
      | itemId | totalQuantity | reservedQuantity |
      | mojito | 1             | 0                |
      | beer   | 1             | 0                |
    When attempting to place an order for:
      | itemId | quantity |
      | mojito | 2        |
      | beer   | 2        |
    Then the order creation is rejected
    And an InsufficientItemInventoryException is raised
    And both inventories remain unchanged
    And no Order is created

  Scenario: Order confirmed reserves items, allowing partial reservations across inventories
    Given the following items are available in inventory:
      | itemId | totalQuantity | reservedQuantity |
      | mojito | 8             | 2                |
      | beer   | 5             | 1                |
    When attempting to place an order for:
      | itemId | quantity |
      | mojito | 3        |
      | beer   | 2        |
    Then an Order is created with status PENDING
    And inventories are updated:
      | itemId | totalQuantity | reservedQuantity | availableQuantity |
      | mojito | 8             | 5                | 3                 |
      | beer   | 5             | 3                | 2                 |

  Scenario: Item reservation is atomic (all or nothing)
    Given the following items are available in inventory:
      | itemId | totalQuantity | reservedQuantity |
      | mojito | 10            | 0                |
      | beer   | 2             | 0                |
    When attempting to place an order for:
      | itemId | quantity |
      | mojito | 5        |
      | beer   | 3        |
    Then the order creation is rejected
    And an InsufficientItemInventoryException is raised for "beer"
    And NO items are reserved (both inventories unchanged)
    And mojito still has availableQuantity = 10
    And beer still has availableQuantity = 2

  Scenario: Stock availability is validated before any persistence
    Given the following items are available in inventory:
      | itemId | totalQuantity | reservedQuantity |
      | mojito | 1             | 0                |
    When attempting to place an order for 2 "mojito"
    Then validation fails immediately
    And no database writes occur
    And no Order entity is created
    And no reservation is made
    And the "mojito" inventory is completely unchanged
