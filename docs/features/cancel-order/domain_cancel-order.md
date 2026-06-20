# Domain: Cancel an Order

**Context**
Festival goers need the ability to cancel orders that are still pending (not yet acknowledged by the bartender). When an order is cancelled, all tokens that were reserved for that order must be unreserved and returned to the festival goer's available balance. Only pending orders can be cancelled; acknowledged orders cannot be cancelled (those require bartender approval for changes via Feature 10).

**Problem**
The Domain module must provide business logic to safely cancel pending orders and release reserved tokens back to the festival goer's available balance. The UseCase must validate the order is pending, unreserve all associated tokens, and change the order status to cancelled.

**Acceptance Criteria**
- [ ] `CancelOrderUseCase` created:
  - Method: `execute(orderId): void`
  - Loads order by ID
  - Validates order status is PENDING
  - Throws `OrderCannotBeCancelledException` if not pending (acknowledged, ready, or already cancelled)
  - Loads festival goer's token balance
  
- [ ] Token unreservation:
  - Retrieve reserved tokens for the order (drink and snack amounts)
  - Call `tokenBalance.unreserveTokens(drinkTokens, snackTokens)`
  - Verify available tokens increase by exact reserved amount
  
- [ ] Order state transition:
  - Update order status to `CANCELLED`
  - Set `updatedAt` timestamp
  
- [ ] Persistence:
  - Save updated token balance via `tokenBalanceRepository.saveTokenBalance(balance)`
  - Save cancelled order via `orderRepository.saveOrder(order)`
  
- [ ] Exception handling:
  - `OrderNotFoundException` if order not found
  - `OrderCannotBeCancelledException` if order not in PENDING state
  - `FestivalGoerNotFoundException` if festival goer not found
  
- [ ] Port definitions in use:
  - `OrderRepository` port: `findOrderById(OrderId)`, `saveOrder(Order)`
  - `TokenBalanceRepository` port: `findTokenBalanceByFestivalGoerId(FestivalGoerId)`, `saveTokenBalance(TokenBalance)`

**Implementation Plan**
1. Create `CancelOrderUseCase` in domain/usecases:
   - Field: `OrderRepository orderRepository`
   - Field: `TokenBalanceRepository tokenBalanceRepository`
   - Method signature:
     ```java
     public void execute(String orderId) throws DomainException
     ```

2. Implement validation logic:
   - Load order: `Order order = orderRepository.findOrderById(orderId).orElseThrow(OrderNotFoundException::new)`
   - Validate state: `if (!order.getStatus().equals(OrderStatus.PENDING)) throw OrderCannotBeCancelledException`
   - Load balance: `TokenBalance balance = tokenBalanceRepository.findTokenBalanceByFestivalGoerId(order.getFestivalGoerId())`

3. Implement token unreservation:
   - Extract reserved amounts: `int drinkTokens = order.getReservedDrinkTokens()`; `int snackTokens = order.getReservedSnackTokens()`
   - Call unreserve: `balance.unreserveTokens(drinkTokens, snackTokens)`
   - Verify available increased: `newAvailable = totalTokens - reservedTokens` (should be higher than before)

4. Implement order status transition:
   - Set status: `order.setStatus(OrderStatus.CANCELLED)`
   - Set timestamp: `order.setUpdatedAt(Instant.now())`

5. Implement persistence:
   - Save balance: `tokenBalanceRepository.saveTokenBalance(balance)`
   - Save order: `orderRepository.saveOrder(order)`
   - Both in same transaction (handled at infrastructure level with `@Transactional`)

6. Define exception in domain/exceptions:
   - `OrderCannotBeCancelledException extends DomainException`

7. Handle both individual and group orders:
   - Works the same way for both; loads festival goer from order context

**Gherkin Scenarios**
Feature: Cancel an Order

Scenario: Cancel pending order and unreserve tokens
  Given a festival goer "fgv-001" with 6 drink tokens total, 3 reserved, 3 available
  And an order "ord-001" with status "PENDING" costing 1 drink token
  And reserved tokens for this order: 1 drink, 0 snack
  When the cancel order use case is executed with orderId "ord-001"
  Then the order status is changed to "CANCELLED"
  And the unreserveTokens method is called with 1 drink, 0 snack
  And the token balance now shows: 6 total, 2 reserved (3 - 1), 4 available (6 - 2)
  And "updatedAt" timestamp is set

Scenario: Cancel pending order with snack tokens reserved
  Given a festival goer "fgv-002" with 3 snack tokens reserved
  And an order "ord-002" with status "PENDING" costing 2 snack tokens
  When the order is cancelled
  Then the unreserveTokens method is called with 0 drink, 2 snack
  And the token balance now shows 1 snack reserved (3 - 2)
  And available snack tokens increased to 8 (9 - 1)

Scenario: Cancel pending order with mixed tokens
  Given a festival goer "fgv-003" with 6 drink total (3 reserved), 9 snack total (4 reserved)
  And an order "ord-003" with status "PENDING" costing 2 drink, 3 snack tokens
  When the order is cancelled
  Then unreserveTokens is called with 2 drink, 3 snack
  And the token balance now shows:
    - Drink: 6 total, 1 reserved (3 - 2), 5 available (6 - 1)
    - Snack: 9 total, 1 reserved (4 - 3), 8 available (9 - 1)

Scenario: Cannot cancel acknowledged order
  Given a festival goer "fgv-004" with an order "ord-004"
  And the order has status "ACKNOWLEDGED"
  When attempting to cancel the order
  Then OrderCannotBeCancelledException is raised
  And the order remains in "ACKNOWLEDGED" status
  And no tokens are unreserved

Scenario: Cannot cancel ready order
  Given an order "ord-005" with status "READY"
  When attempting to cancel the order
  Then OrderCannotBeCancelledException is raised

Scenario: Cannot cancel already cancelled order
  Given an order "ord-006" with status "CANCELLED"
  When attempting to cancel the order again
  Then OrderCannotBeCancelledException is raised

Scenario: Order not found
  Given an order "ord-999" does not exist
  When attempting to cancel order "ord-999"
  Then OrderNotFoundException is raised

Scenario: Festival goer not found
  Given an order "ord-007" references festival goer "fgv-999" (non-existent)
  When attempting to cancel the order
  Then FestivalGoerNotFoundException is raised

Scenario: Cancel order with empty order items
  Given a festival goer "fgv-008" with 6 drink, 9 snack tokens
  And an order "ord-008" with status "PENDING" but no items (edge case)
  And reserved tokens: 0 drink, 0 snack
  When the order is cancelled
  Then the order status is "CANCELLED"
  And unreserveTokens is called with 0, 0
  And token balance remains unchanged

Scenario: Multiple orders - cancel one, others unaffected
  Given a festival goer "fgv-009" with 2 orders: "ord-009a" (reserves 2 drink), "ord-009b" (reserves 1 drink)
  And total reserved: 3 drink, 0 snack
  When order "ord-009a" is cancelled
  Then only "ord-009a" is unreserved (2 drink)
  And "ord-009b" remains reserved (1 drink)
  And total reserved now: 1 drink, 0 snack

**Notes**
- This feature cancels PENDING orders only. Acknowledged orders cannot be cancelled (require Feature 10 bartender approval).
- Token unreservation completely frees the tokens for that order; no partial refunds.
- Both individual and group orders are cancelled the same way.
- The UseCase is purely business logic; HTTP layer (DELETE endpoint) is separate (see application_cancel-order.md).
- Timestamps are updated to track cancellation.
- If cancellation fails (order not found, invalid state), the entire transaction is rolled back (handled at infrastructure level with @Transactional).
- Unreserved tokens immediately become available for the festival goer to use in a new order.
