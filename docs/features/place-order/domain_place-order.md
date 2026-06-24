# Domain: Place an Order (Unified Feature)

**Context**
Festival goers need to place orders for drinks and/or food items. This unified feature covers:
- **Feature 2**: Ordering drinks (non-alcoholic, normal alcoholic, premium alcoholic)
- **Feature 3**: Ordering food (snacks and meals)
- **Feature 4**: Ordering multiple items in a single order

An order costs tokens: drinks consume drink tokens, food items consume snack tokens. The total cost must not exceed the festival goer's current balance.

**Problem**
The Domain module requires rich business models to represent orders with proper validation of:
1. Item types (drinks, snacks, meals) with their respective costs
2. Multiple items in a single order
3. **Item inventory validation**: ensuring all requested items exist in the catalog and sufficient stock is available
4. Token availability validation before order placement (against available tokens, not total)
5. **Concurrent reservation**: both item stock and tokens are reserved atomically when order is placed
6. Order state management

**Acceptance Criteria**
- [ ] `DrinkType` enum created with: `NON_ALCOHOLIC` (0 tokens), `NORMAL_ALCOHOLIC` (1 token), `PREMIUM_ALCOHOLIC` (2 tokens)
- [ ] `FoodType` enum created with: `SNACK` (1 token), `MEAL` (3 tokens)
- [ ] `OrderItem` Value Object created with `type`, `quantity`, and calculated `cost`
- [ ] `ItemId` Value Object created as immutable identifier for items in inventory
- [ ] `ItemInventory` Entity created with:
  - `itemId: ItemId`
  - `totalQuantity: int` (total stock)
  - `reservedQuantity: int` (reserved by pending/acknowledged orders)
  - Method: `getAvailableQuantity(): int` (calculated: totalQuantity - reservedQuantity)
  - Method: `canReserve(int quantity): boolean` (checks if available >= requested)
  - Method: `reserve(int quantity): void` (throws if insufficient, increments reservedQuantity)
  - Invariant: `reservedQuantity <= totalQuantity` always holds
- [ ] `Order` Entity created with:
  - Immutable identity (`OrderId`)
  - List of `OrderItem` (mutable before acknowledgement)
  - `FestivalGoerId` reference
  - Order state (`PENDING`, `ACKNOWLEDGED`, `READY`, `CANCELLED`)
  - `totalDrinkTokensCost` and `totalSnackTokensCost` calculated properties
- [ ] `ItemNotFoundInCatalogException` created (thrown when requested item doesn't exist in inventory)
- [ ] `InsufficientItemInventoryException` created with:
  - `itemId`, `requestedQuantity`, `availableQuantity`
  - (thrown when available stock is insufficient)
- [ ] `ItemAvailabilityService` created (Domain Service for validation):
  - Method: `validateAndReserveItems(List<OrderItem>): void`
  - Validates all items exist and have sufficient available stock
  - Atomically reserves stock for all items (all-or-nothing)
  - Throws ItemNotFoundInCatalogException or InsufficientItemInventoryException on failure
- [ ] `ItemInventoryRepository` Port created with methods:
  - `findByItemId(ItemId): ItemInventory` (or throw ItemNotFoundInCatalogException)
  - `findAllByItemIds(List<ItemId>): List<ItemInventory>`
  - `save(ItemInventory): void`
  - `saveAll(List<ItemInventory>): void`
- [ ] `PlaceOrderUseCase` created for order creation:
  - Validates order items are not empty
  - **Calls `ItemAvailabilityService.validateAndReserveItems(items)` to validate and reserve item stock BEFORE proceeding**
  - Loads festival goer's current token balance (including reserved tokens)
  - Calculates total cost per token type
  - Validates total cost does not exceed **available** tokens (via `tokenBalance.canReserve()`)
  - **Reserves** tokens in festival goer's balance (does NOT deduct yet)
  - Persists order and updated balance
  - Raises domain events (e.g., `OrderPlacedEvent`)
- [ ] Invariants:
  - Order must contain at least 1 item
  - **All requested items must exist in inventory and have sufficient available stock**
  - Order cost cannot exceed festival goer's **available** tokens (separately for drink and food)
  - **Item reservations and token reservations are atomic** (all succeed or all fail, fail-fast on first validation error)
  - Tokens are reserved immediately upon order placement
  - Item stock is only permanently decremented when order is acknowledged (separate feature)
  - Order state transitions are unidirectional and well-defined
- [ ] No framework-specific annotations in Domain models
- [ ] Full test coverage for models, value objects, services, and use case (unit tests)

**Implementation Plan**
1. Create enums in `model` package:
   - `DrinkType` with cost mapping
   - `FoodType` with cost mapping
   
2. Create `OrderItem` Value Object in `model` package:
   - Fields: `drinkType: DrinkType` OR `foodType: FoodType`, `quantity: int`
   - Method: `getDrinkTokenCost(): int` (returns cost if drink, 0 if food)
   - Method: `getSnackTokenCost(): int` (returns cost if food, 0 if drink)
   - Constructor validates quantity > 0
   - Factory methods: `createDrinkItem(DrinkType, int)`, `createFoodItem(FoodType, int)`
   
3. Create `OrderId` Value Object:
   - Immutable identifier

4. Create `ItemId` Value Object in `model` package:
   - Immutable identifier for items in inventory
   - Equals/hashCode implementation

5. Create `ItemInventory` Entity in `model` package:
   - Fields: `itemId: ItemId`, `totalQuantity: int`, `reservedQuantity: int`
   - Constructor validates quantities >= 0 and reservedQuantity <= totalQuantity
   - Methods: `getAvailableQuantity()`, `canReserve(int)`, `reserve(int)`, `releaseReservation(int)`
   
6. Create `Order` Entity in `model` package:
   - Fields: `orderId: OrderId`, `festivalGoerId: FestivalGoerId`, `items: List<OrderItem>`, `status: OrderStatus`
   - Method: `getTotalDrinkTokensCost(): int` (sum of drink items)
   - Method: `getTotalSnackTokensCost(): int` (sum of food items)
   - Method: `validateAgainstAvailableBalance(TokenBalance)` (throws exception if insufficient available tokens)
   - Method: `getItems(): List<OrderItem>` (unmodifiable)
   
7. Create `OrderStatus` enum: `PENDING`, `ACKNOWLEDGED`, `READY`, `CANCELLED`

8. Create exception classes in `exceptions` package:
   - `ItemNotFoundInCatalogException` (thrown when item doesn't exist)
   - `InsufficientItemInventoryException` with fields: `itemId`, `requestedQuantity`, `availableQuantity`
   
9. Define `ItemInventoryRepository` Port in `ports` package:
   - `findByItemId(ItemId): ItemInventory` (or throw ItemNotFoundInCatalogException)
   - `findAllByItemIds(List<ItemId>): List<ItemInventory>`
   - `save(ItemInventory): void`
   - `saveAll(List<ItemInventory>): void`

10. Create `ItemAvailabilityService` in `services` package:
    - Inject: `ItemInventoryRepository`
    - Method: `validateAndReserveItems(List<OrderItem>): void`
    - Steps:
      a. For each OrderItem, extract itemId from item metadata (drinks/food types map to item IDs)
      b. Load all inventories via ItemInventoryRepository.findAllByItemIds()
      c. For each item in order:
         - Throw ItemNotFoundInCatalogException if not found in loaded inventories
         - Throw InsufficientItemInventoryException if canReserve() fails
      d. If all validations pass: call reserve() on each inventory and persist via saveAll()
      e. Transaction boundary: all reservations succeed or all fail (atomicity)
   
11. Create `PlaceOrderUseCase` in `usecases` package:
    - Inject: `ItemAvailabilityService`, `TokenBalanceRepository` (port), `OrderRepository` (port), `EventPublisherPort` (port)
    - Method: `execute(PlaceOrderRequest): PlaceOrderResponse`
    - Steps:
      a. **Validate order items are not empty** (early fail)
      b. **Call `itemAvailabilityService.validateAndReserveItems(items)` to validate catalog and reserve stock** (fail-fast before any further processing)
      c. Load festival goer's token balance via TokenBalanceRepository
      d. Create Order with provided items
      e. Validate order against **available** balance using `tokenBalance.canReserve()`
      f. **Reserve** tokens in balance (update TokenBalance with new reserved values)
      g. Persist order via OrderRepository
      h. Persist updated token balance via TokenBalanceRepository
      i. Publish `OrderPlacedEvent` with order ID and status
      j. Return order ID
   
12. Define outbound ports in `ports` package:
    - `OrderRepository` with methods: `save(Order)`, `findById(OrderId)`
    - `TokenBalanceRepository` (used by this UseCase) with methods: `findTokenBalanceByFestivalGoerId(FestivalGoerId)`, `saveTokenBalance(FestivalGoerId, TokenBalance)`
    - `EventPublisherPort` with `publish(DomainEvent)`
    - Note: `TokenBalanceRepository` is defined and implemented in the "Consult Token Balance" feature ticket
   
13. Create domain event `OrderPlacedEvent`:
    - Fields: `orderId`, `festivalGoerId`, `totalDrinkTokensCost`, `totalSnackTokensCost`, `timestamp`

14. Comprehensive unit tests:
    - Happy path: place order with drinks, food, mixed items
    - Item not found in catalog
    - Insufficient item stock
    - Insufficient drink tokens
    - Insufficient snack tokens
    - Empty order rejection
    - Order cost calculation for complex scenarios
    - **Atomicity**: verify that on item validation failure, no tokens are reserved and no order is created

**Gherkin Scenarios**
Feature: Place an Order (Drinks, Food, Multiple Items)

Scenario: Place order with single non-alcoholic drink
  Given a festival goer with ID "fgv-001"
  And the festival goer has 6 drink tokens and 9 snack tokens
  When placing an order with 1 non-alcoholic drink
  Then an Order is created with status PENDING
  And the order contains 1 drink item
  And the total drink token cost is 0
  And the festival goer's token balance is not changed

Scenario: Place order with single normal alcoholic drink
  Given a festival goer with ID "fgv-001"
  And the festival goer has 6 drink tokens and 9 snack tokens
  When placing an order with 1 normal alcoholic drink
  Then an Order is created with status PENDING
  And the order contains 1 drink item
  And the total drink token cost is 1
  And the festival goer's drink tokens are reserved (1 reserved, 5 available)

Scenario: Place order with premium alcoholic drinks
  Given a festival goer with ID "fgv-001"
  And the festival goer has 6 drink tokens and 9 snack tokens
  When placing an order with 2 premium alcoholic drinks
  Then an Order is created with status PENDING
  And the order contains 2 drink items
  And the total drink token cost is 4
  And the festival goer's drink tokens are reserved (4 reserved, 2 available)

Scenario: Place order with snacks
  Given a festival goer with ID "fgv-001"
  And the festival goer has 6 drink tokens and 9 snack tokens
  When placing an order with 3 snacks
  Then an Order is created with status PENDING
  And the order contains 3 food items
  And the total snack token cost is 3
  And the festival goer's snack tokens are reserved (3 reserved, 6 available)

Scenario: Place order with meals
  Given a festival goer with ID "fgv-001"
  And the festival goer has 6 drink tokens and 9 snack tokens
  When placing an order with 2 meals
  Then an Order is created with status PENDING
  And the order contains 2 food items
  And the total snack token cost is 6
  And the festival goer's snack tokens are reserved (6 reserved, 3 available)

Scenario: Place mixed order with drinks and food
  Given a festival goer with ID "fgv-001"
  And the festival goer has 6 drink tokens and 9 snack tokens
  When placing an order with:
    | Type | Item | Quantity |
    | Drink | Normal Alcoholic | 2 |
    | Drink | Non-Alcoholic | 1 |
    | Food | Snack | 2 |
    | Food | Meal | 1 |
  Then an Order is created with status PENDING
  And the order contains 6 items
  And the total drink token cost is 2
  And the total snack token cost is 5
  And the festival goer's drink tokens are reserved (2 reserved, 4 available)
  And the festival goer's snack tokens are reserved (5 reserved, 4 available)

Scenario: Order rejected due to insufficient drink tokens
  Given a festival goer with ID "fgv-001"
  And the festival goer has 1 drink token and 9 snack tokens
  When placing an order with 3 normal alcoholic drinks
  Then an InsufficientTokensException is raised
  And no Order is created
  And the festival goer's balance is unchanged

Scenario: Order rejected due to insufficient snack tokens
  Given a festival goer with ID "fgv-001"
  And the festival goer has 6 drink tokens and 2 snack tokens
  When placing an order with 3 meals (cost 9 tokens)
  Then an InsufficientTokensException is raised
  And no Order is created
  And the festival goer's balance is unchanged

Scenario: Order rejected due to insufficient both token types
  Given a festival goer with ID "fgv-001"
  And the festival goer has 0 drink tokens and 0 snack tokens
  When placing an order with any items
  Then an InsufficientTokensException is raised
  And no Order is created

Scenario: Cannot place empty order
  Given a festival goer with ID "fgv-001"
  When placing an order with 0 items
  Then an InvalidOrderException is raised
  And no Order is created

Scenario: Order placement publishes OrderPlacedEvent
  Given a festival goer with ID "fgv-001"
  And the festival goer has sufficient tokens
  When placing an order with 2 normal alcoholic drinks and 1 snack
  Then an OrderPlacedEvent is published with:
    | Field | Value |
    | orderId | [generated] |
    | festivalGoerId | fgv-001 |
    | totalDrinkTokensCost | 2 |
    | totalSnackTokensCost | 1 |

Scenario: Order rejected when item not found in catalog
  Given a festival goer with ID "fgv-001"
  And the festival goer has sufficient tokens
  And the catalog does not contain item "unknown-mojito"
  When placing an order for 2 units of "unknown-mojito"
  Then an ItemNotFoundInCatalogException is raised
  And no Order is created
  And the festival goer's balance is unchanged
  And no item stock is reserved

Scenario: Order rejected due to insufficient item stock
  Given a festival goer with ID "fgv-001"
  And the festival goer has sufficient tokens
  And the following items are in inventory:
    | itemId | totalQuantity | reservedQuantity |
    | mojito | 1             | 0                |
  When placing an order for 3 units of "mojito"
  Then an InsufficientItemInventoryException is raised
  And no Order is created
  And the festival goer's balance is unchanged
  And the "mojito" inventory remains unchanged (totalQuantity: 1, reservedQuantity: 0)

Scenario: Order rejected when multiple items, one has insufficient stock
  Given a festival goer with ID "fgv-001"
  And the festival goer has sufficient tokens
  And the following items are in inventory:
    | itemId | totalQuantity | reservedQuantity |
    | beer   | 5             | 0                |
    | water  | 0             | 0                |
  When placing an order with:
    | itemId | quantity |
    | beer   | 2        |
    | water  | 1        |
  Then an InsufficientItemInventoryException is raised
  And no Order is created
  And both inventories remain unchanged
  And the festival goer's balance is unchanged

Scenario: Item reservations are atomic (all-or-nothing)
  Given a festival goer with ID "fgv-001"
  And the festival goer has sufficient tokens
  And the following items are in inventory:
    | itemId | totalQuantity | reservedQuantity |
    | beer   | 10            | 0                |
    | wine   | 1             | 0                |
  When placing an order for:
    | itemId | quantity |
    | beer   | 3        |
    | wine   | 2        |
  Then an InsufficientItemInventoryException is raised
  And no Order is created
  And NO items are reserved (both inventories unchanged)
  And the "beer" inventory still has availableQuantity = 10
  And the "wine" inventory still has availableQuantity = 1

Scenario: Item stock reservation succeeds before token reservation
  Given a festival goer with ID "fgv-001"
  And the festival goer has 6 drink tokens and 9 snack tokens
  And the following items are in inventory:
    | itemId | totalQuantity | reservedQuantity |
    | mojito | 10            | 0                |
  When placing an order for 2 units of "mojito" (normal alcoholic drink)
  Then an Order is created with status PENDING
  And the "mojito" inventory is updated:
    | totalQuantity | reservedQuantity | availableQuantity |
    | 10            | 2                | 8                 |
  And the festival goer's drink tokens are reserved (2 reserved, 4 available)

**Notes**
- This feature mutualizes features 2, 3, and 4 from FEATURES.md into a unified "Place an Order" capability, **with integrated item availability validation**.
- **Item availability validation is a critical prerequisite** before any token reservation or order creation. The sequence is:
  1. Empty order validation (fail-fast)
  2. **Item catalog validation** (all items must exist) → fail-fast, no persistence
  3. **Item stock validation** (sufficient available stock) → fail-fast, no persistence
  4. **Item reservation** (reserves stock atomically for all items)
  5. Token validation and reservation
  6. Order creation and persistence
  7. Domain event publishing
- Ticket covers only Domain layer (models, services, and use case).
- Order state management is kept simple at this stage; state transitions for acknowledge, ready, cancel are handled in separate features.
- Outbound Ports `OrderRepository`, `TokenBalanceRepository`, and `ItemInventoryRepository` will be implemented in Infrastructure module.
- **Token and item reservations** happen synchronously during order placement (neither tokens nor items are deducted yet). Deduction happens when order is acknowledged (separate feature).
- Order items are immutable once placed (changes require separate "Change Order" feature).
- **Item reservation is durable**: reserved items are permanently allocated to the order until the order is acknowledged (consumed) or cancelled (released).
