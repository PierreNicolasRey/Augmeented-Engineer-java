# Domain: Token Balance Model and Query Service

**Context**
Festival goers need to check their remaining token balance at any time. There are two types of tokens: drink tokens and snack tokens. Each festival goer receives a fresh allocation daily and tokens cannot be negative or carried over to the next day.

**Problem**
The Domain module requires rich business models to represent token balances with reservation tracking. Token reservations occur when orders are placed and are released when orders are cancelled or finalized when acknowledged. A Query Service enables efficient token balance retrieval.

**Acceptance Criteria**
- [ ] `TokenBalance` Value Object created with:
  - `totalDrinkTokens` and `totalSnackTokens` (total allocation, never changes after creation)
  - `reservedDrinkTokens` and `reservedSnackTokens` (sum of current order reservations)
  - `availableDrinkTokens` and `availableSnackTokens` (calculated: total - reserved)
  - All values >= 0, reserved <= total
- [ ] `FestivalGoer` Entity created with immutable identity and mutable `TokenBalance`
- [ ] `TokenQueryService` created to retrieve a festival goer's current token balance (including reserved/available breakdown)
- [ ] Invariants:
  - Tokens cannot be negative
  - Reserved tokens cannot exceed total tokens
  - Total tokens reflect daily allocation (6 drink, 9 snack)
  - Tokens are reset daily (business rule documented)
- [ ] Methods on TokenBalance:
  - `canReserve(drinkTokensCost, snackTokensCost): boolean`
  - `reserve(drinkTokensCost, snackTokensCost): TokenBalance` (new instance with updated reserved)
  - `unreserve(drinkTokensCost, snackTokensCost): TokenBalance` (new instance with updated reserved)
  - `confirmReservation(drinkTokensCost, snackTokensCost): TokenBalance` (deducts from total, clears reserved)
- [ ] No framework-specific annotations in Domain models or services
- [ ] Full test coverage for models and query service (unit tests)

**Implementation Plan**
1. Create `TokenBalance` as a Value Object (immutable record)
   - Properties: 
     - `totalDrinkTokens: int` (fixed allocation)
     - `totalSnackTokens: int` (fixed allocation)
     - `reservedDrinkTokens: int` (sum of current order reservations)
     - `reservedSnackTokens: int` (sum of current order reservations)
   - Constructor validates: all >= 0, reserved <= total
   - Calculated properties:
     - `getAvailableDrinkTokens(): int` = totalDrinkTokens - reservedDrinkTokens
     - `getAvailableSnackTokens(): int` = totalSnackTokens - reservedSnackTokens
   - Methods:
     - `canReserve(drinkCost, snackCost): boolean` - checks if reservation possible
     - `reserve(drinkCost, snackCost): TokenBalance` - returns new instance with updated reserved
     - `unreserve(drinkCost, snackCost): TokenBalance` - returns new instance with freed reserved
     - `confirmReservation(drinkCost, snackCost): TokenBalance` - returns new instance with deducted total and cleared reserved
   - Document daily allocation: 6 drink tokens, 9 snack tokens
   
2. Create `FestivalGoer` Entity (rich entity with business logic)
   - Properties: `festivalGoerId: FestivalGoerId` (Value Object), `tokenBalance: TokenBalance`
   - Method: `getTokenBalance(): TokenBalance`
   - Document daily reset rule
   
3. Create `TokenQueryService` in `services` package
   - Method: `consultTokenBalance(FestivalGoerId): TokenBalance`
   - Returns balance with total, reserved, and available breakdown
   - Define outbound port `TokenBalanceRepository` interface
   - Service calls repository and returns TokenBalance
   
4. Define port `TokenBalanceRepository` in `ports` package
   - Method: `findTokenBalanceByFestivalGoerId(FestivalGoerId): TokenBalance`
   - Method: `saveTokenBalance(FestivalGoerId, TokenBalance): void` (for updates)
   - Exception: `FestivalGoerNotFoundException`
   
5. Create comprehensive unit tests covering edge cases

**Gherkin Scenarios**
Feature: Festival Goer Token Balance Inquiry (with Reservations)

Scenario: Retrieve token balance with no reservations
  Given a festival goer with ID "fgv-001"
  And the festival goer has 6 total drink tokens and 9 total snack tokens
  And no orders are placed (no reservations)
  When the token balance is consulted
  Then the balance shows:
    | Type | Total | Reserved | Available |
    | Drink | 6 | 0 | 6 |
    | Snack | 9 | 0 | 9 |

Scenario: Retrieve token balance with partial drink token reservation
  Given a festival goer with ID "fgv-001"
  And the festival goer has 6 total drink tokens and 9 total snack tokens
  And the festival goer has an order reserving 2 drink tokens
  When the token balance is consulted
  Then the balance shows:
    | Type | Total | Reserved | Available |
    | Drink | 6 | 2 | 4 |
    | Snack | 9 | 0 | 9 |

Scenario: Retrieve token balance with multiple order reservations
  Given a festival goer with ID "fgv-001"
  And the festival goer has 6 total drink tokens and 9 total snack tokens
  And the festival goer has:
    | Order | Drink Reserved | Snack Reserved |
    | ord-001 | 2 | 0 |
    | ord-002 | 1 | 3 |
  When the token balance is consulted
  Then the balance shows:
    | Type | Total | Reserved | Available |
    | Drink | 6 | 3 | 3 |
    | Snack | 9 | 3 | 6 |

Scenario: Retrieve token balance with all tokens reserved
  Given a festival goer with ID "fgv-001"
  And the festival goer has 6 total drink tokens and 9 total snack tokens
  And the festival goer has orders reserving all drink tokens and all snack tokens
  When the token balance is consulted
  Then the balance shows:
    | Type | Total | Reserved | Available |
    | Drink | 6 | 6 | 0 |
    | Snack | 9 | 9 | 0 |

Scenario: Cannot create token balance with negative total tokens
  Given I attempt to create a TokenBalance with -1 total drink tokens
  When the TokenBalance is created
  Then an InvalidTokenBalanceException is raised

Scenario: Cannot create token balance with reserved exceeding total
  Given I attempt to create a TokenBalance with 6 total drink tokens but 10 reserved
  When the TokenBalance is created
  Then an InvalidTokenBalanceException is raised

Scenario: Festival goer not found
  Given a non-existent festival goer ID "fgv-999"
  When the token balance is consulted
  Then a FestivalGoerNotFoundException is raised

Scenario: Verify canReserve validation
  Given a TokenBalance with 6 drink tokens, 0 reserved
  When checking canReserve(3, 2)
  Then canReserve returns true
  When checking canReserve(7, 0)
  Then canReserve returns false (exceeds available drink tokens)

**Notes**
- This ticket focuses solely on the Domain layer (models and Query Service).
- The Outbound Port `TokenBalanceRepository` will be implemented in the Infrastructure module.
- Daily token reset logic is documented here but implemented during token initialization (see future "Initialize Token Balance" feature).
- **Key change**: TokenBalance now tracks reserved tokens separately from total. This enables order reservations (place order reserves tokens, cancel order unreserves, acknowledge order confirms/deducts).
- Token reservations prevent double-spending: a festival goer cannot place two orders that together would exceed available tokens.
- All Domain models must remain framework-agnostic (no JPA/Spring annotations).
