# Domain: Acknowledge an Order

**Context**
When a bartender acknowledges an order, it signals that preparation has begun. At this moment, tokens reserved for the order are permanently deducted from the festival goer's balance (not just unreserved, but actually consumed). Additionally, an estimated time of readiness is calculated based on the types and quantities of items in the order. The festival goer receives a notification with the acknowledgement and estimated time. This marks the transition from PENDING to ACKNOWLEDGED state.

**Problem**
The Domain module must provide business logic to acknowledge pending orders, deduct reserved tokens to reflect actual consumption, calculate estimated preparation time based on item composition, and publish a domain event to notify the festival goer.

**Acceptance Criteria**
- [ ] `AcknowledgeOrderUseCase` created:
  - Method: `execute(orderId): void`
  - Loads order by ID
  - Validates order status is PENDING
  - Throws `OrderCannotBeAcknowledgedException` if not pending
  - Loads festival goer's token balance
  
- [ ] Token deduction (consume reserved tokens):
  - Retrieve reserved tokens for the order
  - Call `tokenBalance.consumeTokens(drinkTokens, snackTokens)` (or similar)
  - Reserved tokens are permanently removed (not unreserved, but deducted from total)
  - Verify available tokens decrease by exact reserved amount
  
- [ ] Estimated time calculation:
  - Non-alcoholic drinks: 1 minute **per item** (regardless of type)
  - Normal alcoholic drinks: 2 minutes **per item**
  - Premium alcoholic drinks: 3 minutes **per item**
  - Snacks: 2 minutes **per item** (regardless of type)
  - Meals: 10 minutes **per type** (distinct meal IDs) + longest drink prep time (if drinks present)
  - Total: sum of all category times
  - Examples:
    - 5 non-alcoholic (same type), 1 normal alcoholic = 5×1 + 1×2 = 7 minutes
    - 2 premium (same type), 3 snacks (2 types) = 2×3 + 3×2 = 12 minutes
    - 1 meal + 1 premium drink = 10 + 3 = 13 minutes (meal prep in parallel with longest drink)
  
- [ ] Order state transition:
  - Update order status to `ACKNOWLEDGED`
  - Set `estimatedReadinessTime: Duration` or `estimatedReadinessAt: Instant`
  - Set `updatedAt` timestamp
  
- [ ] Domain event publication:
  - Publish `OrderAcknowledgedEvent` containing:
    - orderId, festivalGoerId, estimatedReadinessTime/Instant, acknowledgementTime
  - Event handler triggers festival goer notification
  
- [ ] Exception handling:
  - `OrderNotFoundException` if order not found
  - `OrderCannotBeAcknowledgedException` if order not in PENDING state
  - `FestivalGoerNotFoundException` if festival goer not found
  - `InsufficientTokensException` if somehow reserved tokens exceed available (sanity check)
  
- [ ] Port definitions in use:
  - `OrderRepository` port: `findOrderById(OrderId)`, `saveOrder(Order)`
  - `TokenBalanceRepository` port: `findTokenBalanceByFestivalGoerId(FestivalGoerId)`, `saveTokenBalance(TokenBalance)`
  - `EventPublisherPort` port: `publish(OrderAcknowledgedEvent)`

**Implementation Plan**
1. Create `AcknowledgeOrderUseCase` in domain/usecases:
   - Field: `OrderRepository orderRepository`
   - Field: `TokenBalanceRepository tokenBalanceRepository`
   - Field: `EventPublisherPort eventPublisher`
   - Method signature:
     ```java
     public void execute(String orderId) throws DomainException
     ```

2. Implement validation logic:
   - Load order: `Order order = orderRepository.findOrderById(orderId).orElseThrow(OrderNotFoundException::new)`
   - Validate state: `if (!order.getStatus().equals(OrderStatus.PENDING)) throw OrderCannotBeAcknowledgedException`
   - Load balance: `TokenBalance balance = tokenBalanceRepository.findTokenBalanceByFestivalGoerId(order.getFestivalGoerId())`

3. Create `EstimatedTimeCalculator` service in domain/services:
   - Method: `calculateEstimatedTimeMinutes(Order): int`
   - **CRITICAL**: Count items, NOT distinct types (except for meals)
   - Logic:
     ```java
     int nonAlcoholicMinutes = order.getItems()
       .filter(item -> item.isNonAlcoholicDrink())
       .count() * 1;
     
     int normalAlcoholicMinutes = order.getItems()
       .filter(item -> item.isNormalAlcoholicDrink())
       .count() * 2;
     
     int premiumAlcoholicMinutes = order.getItems()
       .filter(item -> item.isPremiumAlcoholicDrink())
       .count() * 3;
     
     int snackMinutes = order.getItems()
       .filter(item -> item.isSnack())
       .count() * 2;
     
     // Meals: count by type, not by item quantity
     int mealMinutes = order.getItems()
       .filter(item -> item.isMeal())
       .map(item -> item.getMealType())
       .distinct()
       .count() * 10;
     
     // Calculate longest drink time for parallel meal preparation
     int longestDrinkTime = Math.max(
       nonAlcoholicMinutes,
       Math.max(normalAlcoholicMinutes, premiumAlcoholicMinutes)
     );
     
     int totalTime;
     if (mealMinutes > 0 && longestDrinkTime > 0) {
       // Meals + drinks: meal base (10 min per type) + longest drink time in parallel
       totalTime = mealMinutes + longestDrinkTime + snackMinutes;
     } else if (mealMinutes > 0) {
       // Only meals: base meal time + snacks in parallel
       totalTime = mealMinutes + snackMinutes;
     } else {
       // Only drinks/snacks: all in parallel, take longest
       totalTime = Math.max(
         Math.max(nonAlcoholicMinutes, normalAlcoholicMinutes),
         Math.max(premiumAlcoholicMinutes, snackMinutes)
       );
     }
     
     return totalTime;
     ```

4. Implement token deduction:
   - Extract reserved amounts: `int drinkTokens = order.getReservedDrinkTokens()`; `int snackTokens = order.getReservedSnackTokens()`
   - Call consume: `balance.consumeTokens(drinkTokens, snackTokens)` (decrements totalTokens and reservedTokens by consumed amount)
   - Verify consumed tokens are valid: `drinkTokens >= 0 && snackTokens >= 0`

5. Implement order state transition:
   - Calculate estimated time: `int readinessMinutes = estimatedTimeCalculator.calculateEstimatedTimeMinutes(order)`
   - Set status: `order.setStatus(OrderStatus.ACKNOWLEDGED)`
   - Set readiness: `order.setEstimatedReadinessAt(Instant.now().plusSeconds(readinessMinutes * 60))`
   - Set minutes: `order.setEstimatedReadinessMinutes(readinessMinutes)` (stored for later use without recalculation)
   - Set timestamp: `order.setUpdatedAt(Instant.now())`

6. Implement domain event publication:
   - Create event: `OrderAcknowledgedEvent event = new OrderAcknowledgedEvent(order.getId(), order.getFestivalGoerId(), order.getEstimatedReadinessAt(), Instant.now())`
   - Publish: `eventPublisher.publish(event)`

7. Implement persistence:
   - Save balance: `tokenBalanceRepository.saveTokenBalance(balance)` (reflects consumed tokens)
   - Save order: `orderRepository.saveOrder(order)` (reflects ACKNOWLEDGED status and readiness time)
   - Both in same transaction (handled at infrastructure level with `@Transactional`)

8. Define exceptions in domain/exceptions:
   - `OrderCannotBeAcknowledgedException extends DomainException`

9. Create `OrderAcknowledgedEvent` domain event in domain/events:
   - Field: `orderId: String`
   - Field: `festivalGoerId: String`
   - Field: `estimatedReadinessAt: Instant`
   - Field: `acknowledgedAt: Instant`

**Gherkin Scenarios**
Feature: Acknowledge an Order

Scenario: Acknowledge pending order with non-alcoholic drink
  Given a festival goer "fgv-001" with 6 drink tokens total, 0 reserved
  And an order "ord-001" with status "PENDING" containing 1 non-alcoholic drink (cost 0 drink tokens)
  And reserved tokens for this order: 0 drink, 0 snack
  When the acknowledge order use case is executed
  Then the order status is changed to "ACKNOWLEDGED"
  And the estimated readiness time is 1 minute
  And consumeTokens is NOT called (no tokens deducted)
  And OrderAcknowledgedEvent is published
  And the token balance remains unchanged

Scenario: Acknowledge pending order with alcoholic drink
  Given an order "ord-002" containing 1 normal alcoholic drink (cost 1 drink token)
  And reserved tokens: 1 drink, 0 snack
  When the order is acknowledged
  Then consumeTokens is called with 1 drink, 0 snack
  And the token balance shows 1 drink consumed (total decreased)
  And the estimated readiness time is 2 minutes (1 type × 2 minutes)
  And estimatedReadinessAt is set to approximately now + 2 minutes

Scenario: Acknowledge order with multiple drink items (same types)
  Given an order containing:
    - 5 non-alcoholic drinks (same type, quantity 5)
    - 1 normal alcoholic drink
  When the order is acknowledged
  Then the estimated readiness time is: 5×1 + 1×2 = 7 minutes (per item, not per type)

Scenario: Acknowledge order with snacks (multiple types)
  Given an order containing:
    - 2 snacks of type "chips"
    - 3 snacks of type "cookie"
  When the order is acknowledged
  Then the estimated readiness time is: (2+3)×2 = 10 minutes (all snacks counted by item)

Scenario: Acknowledge order with meals and drinks
  Given an order containing:
    - 1 meal type "burger" (1 type only)
    - 1 premium alcoholic drink
  When the order is acknowledged
  Then the estimated readiness time is: 10 (meal) + 3 (premium drink in parallel) = 13 minutes
  And meals and drinks are prepared in parallel

Scenario: Acknowledge order with meals, snacks, and drinks
  Given an order containing:
    - 1 meal type "burger"
    - 2 snacks
    - 1 premium drink
  When the order is acknowledged
  Then the estimated readiness time is: 10 (meal) + 3 (longest drink) + 4 (snacks) = 17 minutes
  And meal is in parallel with longest drink, all snacks in series with drinks

Scenario: Acknowledge order and tokens deducted correctly
  Given a festival goer "fgv-003" with 6 drink tokens total
  And an order "ord-003" with 2 premium alcoholic drinks (cost 2 × 2 = 4 drink tokens, reserved 4)
  And current reserved for this order: 4 drink, 0 snack
  And other reserved elsewhere: 0 drink, 0 snack
  When the order is acknowledged
  Then consumeTokens is called with 4 drink, 0 snack
  And the token balance shows: 6 total, 0 reserved (all consumed), 2 available (6 - 4)

Scenario: Cannot acknowledge non-pending order
  Given an order "ord-004" with status "ACKNOWLEDGED"
  When attempting to acknowledge the order again
  Then OrderCannotBeAcknowledgedException is raised
  And no tokens are consumed
  And no event is published

Scenario: Cannot acknowledge ready order
  Given an order "ord-005" with status "READY"
  When attempting to acknowledge the order
  Then OrderCannotBeAcknowledgedException is raised

Scenario: Cannot acknowledge cancelled order
  Given an order "ord-006" with status "CANCELLED"
  When attempting to acknowledge the order
  Then OrderCannotBeAcknowledgedException is raised

Scenario: Order not found
  Given an order "ord-999" does not exist
  When attempting to acknowledge order "ord-999"
  Then OrderNotFoundException is raised

Scenario: Festival goer not found
  Given an order "ord-007" references festival goer "fgv-999" (non-existent)
  When attempting to acknowledge the order
  Then FestivalGoerNotFoundException is raised

Scenario: OrderAcknowledgedEvent contains correct data
  Given an order "ord-008" is acknowledged at time T1
  When the domain event is published
  Then the event contains:
    - orderId: "ord-008"
    - festivalGoerId: (correct ID)
    - estimatedReadinessAt: approximately T1 + calculated minutes
    - acknowledgedAt: approximately T1

**Notes**
- Acknowledged orders cannot revert to PENDING; they must be marked READY or otherwise handled by the bartender.
- Token deduction happens at acknowledgement, not at order placement (reservation happens on placement).
- Estimated time calculation is based on **types** of items, not quantities (e.g., 3 chips of same type = 2 min, not 6 min).
- Meals and drinks are prepared in parallel, so meal prep time (10 min) + longest drink time, not added sequentially.
- Domain events decouple order acknowledgement from festival goer notification (event handler sends the notification).
- The UseCase is purely business logic; HTTP layer (PUT/PATCH endpoint) is separate (see application_acknowledge-order.md).
- If acknowledgement fails (order not found, state invalid, token deduction fails), the entire transaction is rolled back.
