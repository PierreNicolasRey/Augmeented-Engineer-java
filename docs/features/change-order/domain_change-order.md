# Domain: Change an Order

**Context**
Festival goers need the ability to modify pending orders by adding or removing items before the bartender has acknowledged the order. When an order is changed, the reserved tokens must be recalculated to ensure the new total cost does not exceed the festival goer's available balance. If an order has already been acknowledged, the change request must be rejected to prevent conflicts with preparation.

**Problem**
The Domain module must provide business logic to safely modify pending orders while maintaining token reservation consistency. The UseCase must validate that the order is in PENDING state, recalculate reserved tokens, and reject changes if the new cost exceeds available tokens.

**Acceptance Criteria**
- [ ] `ChangeOrderUseCase` created:
  - Method: `execute(orderId, itemsToAdd, itemsToRemove): void`
  - Loads order by ID
  - Validates order status is PENDING
  - Throws `OrderCannotBeChangedException` if acknowledged/ready/cancelled
  - Loads festival goer's current token balance
  
- [ ] Order items modification:
  - Add new items to the order
  - Remove existing items by ID
  - Recalculate total order cost (sum of all items)
  
- [ ] Token reservation recalculation:
  - Calculate new reserved amount: `newReserved = newTotalCost`
  - Calculate available tokens: `available = totalTokens - (reservedBefore - oldOrderCost) = totalTokens - reservedForOtherOrders`
  - Validate: `newTotalCost <= available`
  - If insufficient tokens, throw `InsufficientTokensException` with breakdown
  
- [ ] Exception handling:
  - `OrderNotFoundException` if order not found
  - `OrderCannotBeChangedException` if order not in PENDING state
  - `InsufficientTokensException` if new total exceeds available tokens
  - `FestivalGoerNotFoundException` if festival goer not found
  
- [ ] Token balance update:
  - Call `tokenBalanceRepository.findTokenBalanceByFestivalGoerId(festivalGoerId)`
  - Call `unreserveTokens()` for the OLD order cost
  - Call `reserveTokens()` for the NEW order cost
  - Persist updated balance via `tokenBalanceRepository.saveTokenBalance(balance)`
  
- [ ] Order persistence:
  - Update order items list
  - Set `updatedAt` timestamp
  - Persist via `orderRepository.saveOrder(order)`
  
- [ ] Port definitions in use:
  - `OrderRepository` port: `findOrderById(OrderId)`, `saveOrder(Order)`
  - `TokenBalanceRepository` port: `findTokenBalanceByFestivalGoerId(FestivalGoerId)`, `saveTokenBalance(TokenBalance)`

**Implementation Plan**
1. Create `ChangeOrderUseCase` in domain/usecases:
   - Field: `OrderRepository orderRepository`
   - Field: `TokenBalanceRepository tokenBalanceRepository`
   - Method signature:
     ```java
     public void execute(String orderId, List<OrderItemRequest> itemsToAdd, List<String> itemIdsToRemove) throws DomainException
     ```

2. Implement validation logic:
   - Load order: `Order order = orderRepository.findOrderById(orderId).orElseThrow(OrderNotFoundException::new)`
   - Validate state: `if (!order.getStatus().equals(OrderStatus.PENDING)) throw OrderCannotBeChangedException`
   - Load balance: `TokenBalance balance = tokenBalanceRepository.findTokenBalanceByFestivalGoerId(order.getFestivalGoerId())`

3. Implement item modification:
   - Remove items from order: iterate itemIdsToRemove, call `order.removeItem(itemId)`
   - Add items to order: iterate itemsToAdd, call `order.addItem(item)`
   - Recalculate order cost: `order.calculateTotalCost()`

4. Implement token recalculation:
   - Old cost: `int oldReservedDrinkTokens = order.getReservedDrinkTokens()` (before change)
   - New cost: `int newTotalCost = order.calculateTotalCost()` (after add/remove)
   - Available: `availableDrinkTokens = balance.getTotalDrinkTokens() - balance.getReservedDrinkTokens() + oldReservedDrinkTokens`
   - Validation: `if (newTotalCost > available) throw InsufficientTokensException`

5. Implement token balance update:
   - Unreserve old amount: `balance.unreserveTokens(oldReservedDrinkTokens, oldReservedSnackTokens)`
   - Reserve new amount: `balance.reserveTokens(newReservedDrinkTokens, newReservedSnackTokens)`
   - Persist: `tokenBalanceRepository.saveTokenBalance(balance)`

6. Implement order persistence:
   - Set updatedAt: `order.setUpdatedAt(Instant.now())`
   - Persist: `orderRepository.saveOrder(order)`

7. Define exceptions in domain/exceptions:
   - `OrderCannotBeChangedException extends DomainException`
   - `InsufficientTokensException extends DomainException` (reuse from place-order if available)

8. Handle both individual and group orders:
   - Applicable to both; load festival goer from order context
   - Token balance lookup uses festival goer ID (same for both)

**Gherkin Scenarios**
Feature: Change an Order

Scenario: Add item to pending order with sufficient tokens
  Given a festival goer "fgv-001" with 3 drink tokens and 9 snack tokens
  And an order "ord-001" with status "PENDING" containing 1 drink (alcoholic, cost 1 drink token)
  And reserved tokens are: 1 drink, 0 snack
  When adding an item (1 snack, cost 1 snack token) to the order
  And recalculating reserved tokens
  Then the new reserved tokens are: 1 drink, 1 snack
  And available tokens remain: 2 drink, 8 snack
  And the order is successfully updated
  And "updatedAt" timestamp is set

Scenario: Remove item from pending order
  Given a festival goer "fgv-002" with 6 drink tokens and 9 snack tokens
  And an order "ord-002" with status "PENDING" containing 2 items (1 alcoholic drink cost 1, 1 snack cost 1)
  And reserved tokens are: 1 drink, 1 snack
  When removing the snack item
  And recalculating reserved tokens
  Then the new reserved tokens are: 1 drink, 0 snack
  And available tokens remain: 5 drink, 9 snack
  And the order is successfully updated

Scenario: Add item but insufficient tokens
  Given a festival goer "fgv-003" with 2 drink tokens and 3 snack tokens
  And an order "ord-003" with status "PENDING" containing 1 premium drink (cost 2 drink tokens)
  And reserved tokens are: 2 drink, 0 snack
  And available tokens are: 0 drink, 3 snack
  When adding a snack (cost 1 snack token) - OK
  And adding another item (cost 1 snack token) - total snack cost 2
  Then the order is updated with 1 snack
  And the order can accept one more snack (total cost 2)
  But adding a 3rd snack should fail (cost 1, but available 1)
  And InsufficientTokensException is raised with breakdown

Scenario: Cannot change acknowledged order
  Given a festival goer "fgv-004" with 6 drink tokens and 9 snack tokens
  And an order "ord-004" with status "ACKNOWLEDGED" containing 1 drink (cost 1 drink token)
  When attempting to add an item to the order
  Then OrderCannotBeChangedException is raised
  And the order remains unchanged

Scenario: Cannot change ready order
  Given a festival goer "fgv-005" with 6 drink tokens and 9 snack tokens
  And an order "ord-005" with status "READY" containing 1 drink (cost 1 drink token)
  When attempting to remove an item from the order
  Then OrderCannotBeChangedException is raised

Scenario: Cannot change cancelled order
  Given a festival goer "fgv-006" with 6 drink tokens and 9 snack tokens
  And an order "ord-006" with status "CANCELLED" containing 1 drink (cost 1 drink token)
  When attempting to modify the order
  Then OrderCannotBeChangedException is raised

Scenario: Replace items in order (remove old, add new)
  Given a festival goer "fgv-007" with 3 drink tokens and 9 snack tokens
  And an order "ord-007" with status "PENDING" containing 1 non-alcoholic drink (cost 0)
  And reserved tokens are: 0 drink, 0 snack
  When removing the non-alcoholic drink
  And adding 1 premium alcoholic drink (cost 2 drink tokens)
  And recalculating reserved tokens
  Then the new reserved tokens are: 2 drink, 0 snack
  And available tokens remain: 1 drink, 9 snack
  And the order is successfully updated

Scenario: Add multiple items at once
  Given a festival goer "fgv-008" with 2 drink tokens and 9 snack tokens
  And an order "ord-008" with status "PENDING" containing nothing (empty)
  And reserved tokens are: 0 drink, 0 snack
  When adding 3 items: 1 snack (cost 1), 1 snack (cost 1), 1 drink (cost 1 drink token)
  Then the order contains 3 items
  And reserved tokens are: 1 drink, 2 snack
  And available tokens remain: 1 drink, 7 snack

Scenario: Order not found
  Given an order "ord-999" does not exist
  When attempting to change order "ord-999"
  Then OrderNotFoundException is raised

Scenario: Festival goer not found
  Given a festival goer "fgv-999" does not exist
  And an order "ord-010" references festival goer "fgv-999"
  When attempting to change the order
  Then FestivalGoerNotFoundException is raised

**Notes**
- This feature modifies PENDING orders only. Acknowledged orders are blocked to prevent preparation conflicts.
- Token reservation is recalculated based on the new order total cost.
- Available tokens = total - (reserved for other orders), so recalculation is: `newAvailable = total - (reserved_now - oldOrderCost)`
- Both individual and group orders can be changed using the same UseCase logic.
- The UseCase is purely business logic; HTTP layer (PATCH endpoint) is separate (see application_change-order.md).
- Timestamps are updated automatically to track modifications.
- If modification fails (insufficient tokens, order state invalid), the entire transaction is rolled back (handled at infrastructure level with @Transactional).
