# Domain: Token Balance Model and Query Service

**Context**
Festival goers need to check their remaining token balance at any time. There are two types of tokens: drink tokens and snack tokens. Each festival goer receives a fresh allocation daily and tokens cannot be negative or carried over to the next day.

**Problem**
The Domain module requires rich business models to represent token balances and a Query Service to enable efficient token balance retrieval without exposing mutation logic.

**Acceptance Criteria**
- [ ] `TokenBalance` Value Object created with `drinkTokens` and `snackTokens` properties (both >= 0)
- [ ] `FestivalGoer` Entity created with immutable identity and mutable `TokenBalance`
- [ ] `TokenQueryService` created to retrieve a festival goer's current token balance
- [ ] Invariant: tokens cannot be negative (enforced in Value Object constructor)
- [ ] Invariant: daily token allocation is 9 food tokens and 6 drink tokens (business rule documented)
- [ ] Invariant: tokens are reset daily (business rule documented)
- [ ] No framework-specific annotations in Domain models or services
- [ ] Full test coverage for models and query service (unit tests in domain-testing module)

**Implementation Plan**
1. Create `TokenBalance` as a Value Object (immutable record)
   - Properties: `drinkTokens: int`, `snackTokens: int`
   - Constructor validates non-negative values
   - Document daily allocation: 6 drink tokens, 9 snack tokens
   
2. Create `FestivalGoer` Entity (rich entity with business logic)
   - Properties: `festivalGoerId: FestivalGoerId` (Value Object), `tokenBalance: TokenBalance`
   - Method: `getTokenBalance(): TokenBalance`
   - Document daily reset rule
   
3. Create `TokenQueryService` in `services` package
   - Method: `consultTokenBalance(FestivalGoerId): TokenBalance`
   - Define outbound port `TokenBalanceRepository` interface
   - Service calls repository and returns TokenBalance
   
4. Define port `TokenBalanceRepository` in `ports` package
   - Method: `findTokenBalanceByFestivalGoerId(FestivalGoerId): TokenBalance`
   - Exception: `FestivalGoerNotFoundException`
   
5. Create comprehensive unit tests covering edge cases

**Gherkin Scenarios**
Feature: Festival Goer Token Balance Inquiry

Scenario: Retrieve token balance for a festival goer with positive balance
  Given a festival goer with ID "fgv-001"
  And the festival goer has 5 drink tokens
  And the festival goer has 7 snack tokens
  When the token balance is consulted
  Then the balance shows 5 drink tokens
  And the balance shows 7 snack tokens

Scenario: Retrieve token balance with zero tokens
  Given a festival goer with ID "fgv-002"
  And the festival goer has 0 drink tokens
  And the festival goer has 0 snack tokens
  When the token balance is consulted
  Then the balance shows 0 drink tokens
  And the balance shows 0 snack tokens

Scenario: Cannot create token balance with negative drink tokens
  Given I attempt to create a TokenBalance with -1 drink tokens
  When the TokenBalance is created
  Then an InvalidTokenBalanceException is raised

Scenario: Cannot create token balance with negative snack tokens
  Given I attempt to create a TokenBalance with -1 snack tokens
  When the TokenBalance is created
  Then an InvalidTokenBalanceException is raised

Scenario: Festival goer not found
  Given a non-existent festival goer ID "fgv-999"
  When the token balance is consulted
  Then a FestivalGoerNotFoundException is raised

**Notes**
- This ticket focuses solely on the Domain layer (models and Query Service).
- The Outbound Port `TokenBalanceRepository` will be implemented in the Infrastructure module.
- Daily token reset logic is documented here but implemented during token initialization (see future "Initialize Token Balance" feature).
- All Domain models must remain framework-agnostic (no JPA/Spring annotations).
