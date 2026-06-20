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
3. Token availability validation before order placement (against available tokens, not total)
4. Token reservation when order is placed
5. Order state management

**Acceptance Criteria**
- [ ] `DrinkType` enum created with: `NON_ALCOHOLIC` (0 tokens), `NORMAL_ALCOHOLIC` (1 token), `PREMIUM_ALCOHOLIC` (2 tokens)
- [ ] `FoodType` enum created with: `SNACK` (1 token), `MEAL` (3 tokens)
- [ ] `OrderItem` Value Object created with `type`, `quantity`, and calculated `cost`
- [ ] `Order` Entity created with:
  - Immutable identity (`OrderId`)
  - List of `OrderItem` (mutable before acknowledgement)
  - `FestivalGoerId` reference
  - Order state (`PENDING`, `ACKNOWLEDGED`, `READY`, `CANCELLED`)
  - `totalDrinkTokensCost` and `totalSnackTokensCost` calculated properties
- [ ] `PlaceOrderUseCase` created for order creation:
  - Validates order items are not empty
  - Loads festival goer's current token balance (including reserved tokens)
  - Calculates total cost per token type
  - Validates total cost does not exceed **available** tokens (via `tokenBalance.canReserve()`)
  - **Reserves** tokens in festival goer's balance (does NOT deduct yet)
  - Persists order and updated balance
  - Raises domain events (e.g., `OrderPlacedEvent`)
- [ ] Invariants:
  - Order must contain at least 1 item
  - Order cost cannot exceed festival goer's **available** tokens (separately for drink and food)
  - Tokens are reserved immediately upon order placement
  - Tokens are only deducted when order is acknowledged (separate feature)
  - Order state transitions are unidirectional and well-defined
- [ ] No framework-specific annotations in Domain models
- [ ] Full test coverage for models, value objects, and use case (unit tests)

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
   
4. Create `Order` Entity in `model` package:
   - Fields: `orderId: OrderId`, `festivalGoerId: FestivalGoerId`, `items: List<OrderItem>`, `status: OrderStatus`
   - Method: `getTotalDrinkTokensCost(): int` (sum of drink items)
   - Method: `getTotalSnackTokensCost(): int` (sum of food items)
   - Method: `validateAgainstAvailableBalance(TokenBalance)` (throws exception if insufficient available tokens)
   - Method: `getItems(): List<OrderItem>` (unmodifiable)
   
5. Create `OrderStatus` enum: `PENDING`, `ACKNOWLEDGED`, `READY`, `CANCELLED`
   
6. Create `PlaceOrderUseCase` in `usecases` package:
   - Inject: `TokenBalanceRepository` (port), `OrderRepository` (port), `EventPublisherPort` (port)
   - Method: `execute(PlaceOrderRequest): PlaceOrderResponse`
   - Steps:
     a. Load festival goer's token balance via TokenBalanceRepository
     b. Create Order with provided items
     c. Validate order against **available** balance using `tokenBalance.canReserve()`
     d. **Reserve** tokens in balance (update TokenBalance with new reserved values)
     e. Persist order via OrderRepository
     f. Persist updated token balance via TokenBalanceRepository
     g. Publish `OrderPlacedEvent` with order ID and status
     h. Return order ID
   
7. Define outbound ports in `ports` package:
   - `OrderRepository` with methods: `save(Order)`, `findById(OrderId)`
   - `TokenBalanceRepository` (used by this UseCase) with methods: `findTokenBalanceByFestivalGoerId(FestivalGoerId)`, `saveTokenBalance(FestivalGoerId, TokenBalance)`
   - `EventPublisherPort` with `publish(DomainEvent)`
   - Note: `TokenBalanceRepository` is defined and implemented in the "Consult Token Balance" feature ticket
   
8. Create domain event `OrderPlacedEvent`:
   - Fields: `orderId`, `festivalGoerId`, `totalDrinkTokensCost`, `totalSnackTokensCost`, `timestamp`

9. Comprehensive unit tests:
   - Happy path: place order with drinks, food, mixed items
   - Insufficient drink tokens
   - Insufficient snack tokens
   - Empty order rejection
   - Order cost calculation for complex scenarios

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

**Notes**
- This feature mutualizes features 2, 3, and 4 from FEATURES.md into a unified "Place an Order" capability.
- Ticket covers only Domain layer (models and use case).
- Order state management is kept simple at this stage; state transitions for acknowledge, ready, cancel are handled in separate features.
- Outbound Port `OrderRepository` will be implemented in Infrastructure module.
- **Token reservation** happens synchronously during order placement (tokens are reserved, not deducted). Token deduction happens when order is acknowledged (separate feature).
- Order items are immutable once placed (changes require separate "Change Order" feature).
