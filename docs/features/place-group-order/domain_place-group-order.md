# Domain: Place a Group Order

**Context**
Festival goers need to pool their tokens to place orders collectively. A group order allows multiple festival goers to contribute tokens (up to their available balance) to a shared order. The contributed tokens are reserved from each contributor's balance, and the order is created as a single unit with references to all contributors and their individual contributions.

**Problem**
The Domain module requires rich business models to represent group orders with:
1. Multiple contributors with individual token contributions
2. Validation that pooled available tokens are sufficient
3. Individual token reservation per contributor
4. Group order state management matching single orders

**Acceptance Criteria**
- [ ] `TokenContribution` Value Object created with:
  - `contributorId: FestivalGoerId`
  - `drinkTokensContributed: int`
  - `snackTokensContributed: int`
  - All values >= 0
  
- [ ] `GroupOrder` Entity created with:
  - Immutable identity (`OrderId`)
  - List of `OrderItem` (same as individual orders)
  - List of `TokenContribution` (contributors and amounts)
  - `totalDrinkTokensCost` and `totalSnackTokensCost`
  - Order state (`PENDING`, `ACKNOWLEDGED`, `READY`, `CANCELLED`)
  - `isGroupOrder(): boolean` flag
  
- [ ] `PlaceGroupOrderUseCase` created for group order creation:
  - Accepts list of contributors with their ID and optional contribution limits
  - Validates order items are not empty
  - Loads each contributor's token balance (all available tokens, not reserved)
  - Calculates total cost per token type
  - **Smart contribution allocation**: if individual limits not specified, distribute cost proportionally across contributors' available tokens
  - Validates total available tokens (sum across all contributors) >= total cost
  - **Reserves tokens individually** for each contributor
  - Persists group order with contribution records
  - Persists updated balances for all contributors
  - Raises domain events (e.g., `GroupOrderPlacedEvent` with contributor list)
  
- [ ] Invariants:
  - Group order must have at least 2 contributors
  - Group order must contain at least 1 item
  - Sum of all contributors' available tokens must >= order cost
  - Each contributor's contribution cannot exceed their available tokens
  - Tokens are reserved per contributor immediately upon order placement
  - A festival goer cannot contribute more than their available tokens
  
- [ ] Exception types:
  - `InsufficientGroupTokensException` (sum of available < cost)
  - `InvalidContributionException` (individual contribution > available)
  - `InvalidGroupOrderException` (< 2 contributors, or other structural issues)
  
- [ ] No framework-specific annotations in Domain models
- [ ] Full test coverage for models, value objects, and use case (unit tests)

**Implementation Plan**
1. Create `TokenContribution` Value Object in `model` package:
   - Fields: `contributorId: FestivalGoerId`, `drinkTokensContributed: int`, `snackTokensContributed: int`
   - Constructor validates all values >= 0
   - Method: `getTotalTokensContributed(): int`
   
2. Create `GroupOrder` Entity in `model` package:
   - Fields: `orderId: OrderId`, `items: List<OrderItem>`, `contributions: List<TokenContribution>`, `status: OrderStatus`
   - Method: `getTotalDrinkTokensCost(): int`
   - Method: `getTotalSnackTokensCost(): int`
   - Method: `isGroupOrder(): boolean` (returns true)
   - Method: `getContributors(): List<FestivalGoerId>` (unmodifiable)
   - Method: `getContributionFor(FestivalGoerId): TokenContribution` (optional)
   
3. Create `ContributionAllocationStrategy` interface:
   - Method: `allocate(List<TokenBalance>, int drinkCost, int snackCost): List<TokenContribution>`
   - Default implementation: `ProportionalContributionStrategy`
     - Distributes cost proportionally based on each contributor's available tokens
     - Ensures no contributor contributes more than available
   
4. Create `PlaceGroupOrderUseCase` in `usecases` package:
   - Inject: `TokenBalanceRepository`, `OrderRepository`, `EventPublisherPort`, `ContributionAllocationStrategy`
   - Method: `execute(PlaceGroupOrderRequest): PlaceGroupOrderResponse`
   - Steps:
     a. Load all contributors' token balances
     b. Validate at least 2 contributors
     c. Create GroupOrder with items
     d. Validate order not empty
     e. Calculate total cost per token type
     f. Allocate contributions (proportional or custom)
     g. Validate allocated contributions don't exceed available per contributor
     h. Validate total pooled contribution >= total cost
     i. For each contributor: reserve their allocated tokens in balance
     j. Persist group order with all contributions
     k. Persist updated balances for all contributors
     l. Publish `GroupOrderPlacedEvent` with contributors and contributions
     m. Return order ID
   
5. Create domain event `GroupOrderPlacedEvent`:
   - Fields: `orderId`, `contributions: List<TokenContribution>`, `totalDrinkTokensCost`, `totalSnackTokensCost`, `timestamp`
   
6. Define/extend outbound ports in `ports` package:
   - `TokenBalanceRepository` (already exists, used for each contributor)
   - `OrderRepository` (already exists, used to save group order)
   
7. Comprehensive unit tests:
   - Happy path: group order with 2+ contributors, equal distribution
   - Proportional distribution scenarios
   - Insufficient total tokens
   - Individual contributor exceeding available
   - Single contributor edge case (should fail, need >= 2)
   - Custom contribution validation

**Gherkin Scenarios**
Feature: Place a Group Order

Scenario: Successfully place group order with 2 contributors equally pooling
  Given a festival goer "fgv-001" with 6 drink tokens, 0 reserved
  And a festival goer "fgv-002" with 4 drink tokens, 0 reserved
  When placing a group order with 2 normal alcoholic drinks (cost 2 drink tokens)
  And contributors are "fgv-001" and "fgv-002"
  Then a GroupOrder is created with status PENDING
  And the order contains 2 drink items
  And "fgv-001" contributes 1 drink token (1 reserved, 5 available)
  And "fgv-002" contributes 1 drink token (1 reserved, 3 available)

Scenario: Successfully place group order with proportional distribution
  Given a festival goer "fgv-001" with 6 drink tokens, 0 reserved
  And a festival goer "fgv-002" with 3 drink tokens, 0 reserved
  And a festival goer "fgv-003" with 3 drink tokens, 0 reserved
  When placing a group order with 6 normal alcoholic drinks (cost 6 drink tokens)
  And contributors are "fgv-001", "fgv-002", "fgv-003"
  Then a GroupOrder is created
  And "fgv-001" contributes 3 drink tokens (3 reserved, 3 available)
  And "fgv-002" contributes 1.5 drink tokens (rounded appropriately)
  And "fgv-003" contributes 1.5 drink tokens (rounded appropriately)

Scenario: Successfully place mixed group order with multiple item types
  Given festival goers with various available tokens
  When placing a group order with drinks and food
  Then tokens are reserved per contributor per type
  And reservation is per token type (drink and snack separately)

Scenario: Group order rejected with insufficient pooled tokens
  Given a festival goer "fgv-001" with 2 drink tokens, 0 reserved
  And a festival goer "fgv-002" with 1 drink token, 0 reserved
  When placing a group order with 5 normal alcoholic drinks (cost 5 drink tokens)
  And contributors are "fgv-001" and "fgv-002"
  Then an InsufficientGroupTokensException is raised
  And no GroupOrder is created
  And no tokens are reserved

Scenario: Individual contributor exceeding available tokens
  Given a festival goer "fgv-001" with 2 drink tokens, 0 reserved
  And a festival goer "fgv-002" with 1 drink token, 0 reserved
  When placing a group order with custom contributions:
    | Contributor | Drink Contribution | Snack Contribution |
    | fgv-001 | 1 | 0 |
    | fgv-002 | 5 | 0 |
  Then an InvalidContributionException is raised
  And no GroupOrder is created

Scenario: Group order requires at least 2 contributors
  Given a festival goer "fgv-001" with 6 drink tokens, 0 reserved
  When attempting to place a group order with only 1 contributor
  Then an InvalidGroupOrderException is raised

Scenario: Group order placed with pre-defined contributions
  Given a festival goer "fgv-001" with 6 drink tokens, 0 reserved
  And a festival goer "fgv-002" with 4 drink tokens, 0 reserved
  When placing a group order with custom contributions:
    | Contributor | Drink Contribution | Snack Contribution |
    | fgv-001 | 1 | 0 |
    | fgv-002 | 1 | 0 |
  Then the GroupOrder is created with exactly those contributions
  And no proportional distribution is applied

Scenario: Group order publishes event with all contributors
  Given valid contributors and sufficient tokens
  When placing a group order
  Then a GroupOrderPlacedEvent is published containing:
    | Field | Includes |
    | orderId | [generated] |
    | contributions | All contributors and amounts |
    | totalDrinkTokensCost | 2 |
    | totalSnackTokensCost | 3 |

**Notes**
- Feature 5 mutualizes the concept of "pooling" from FEATURES.md.
- Ticket covers only Domain layer (models and use case).
- **Token reservation is per contributor**: each contributor's balance is updated individually.
- Proportional distribution strategy is default but pluggable via `ContributionAllocationStrategy`.
- Group orders are treated as single orders in the rest of the system (acknowledge, mark ready, cancel).
- If any contributor's reservation fails (e.g., concurrent order placement), the entire group order placement must fail atomically (transaction management in Infrastructure).
- Future feature: ability to specify custom contribution limits per contributor.
