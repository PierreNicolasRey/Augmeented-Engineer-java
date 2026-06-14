# Issue: Prevent negative token balances for festival goers

## Context
The current feature for consulting a festival goer's remaining token balance must ensure that no user can have a negative quantity of drink or snack tokens.

## Problem
A festival goer may end up with a negative token balance due to missing validation or incorrect token accounting during token issuance, transfers, or order placement.

## Success Criteria
- The domain model must prevent creation or persistence of any festival goer state with negative drink or snack tokens.
- Any token operation (issuance, transfer, order placement, cancellation) must enforce non-negative balances.
- The validation must be expressed in domain invariants and tested at the domain layer.

## Implementation Plan
1. Review the festival goer token model and identify where drink/snack balances are updated.
2. Add domain-level validation to reject negative token adjustments.
3. Add unit tests covering:
   - direct balance updates to negative values
   - transfer that would cause negative balance
   - order placement that would exceed available tokens
   - cancellation and refund behavior that preserves non-negative balance
4. Ensure any persistence adapter or mapper preserves the invariant.

## Gherkin Scenarios
### Scenario: Prevent negative drink token balance on issuance
Given a festival goer with 0 drink tokens
When the system attempts to reduce their drink tokens by 1
Then the operation is rejected and the drink token balance remains 0

### Scenario: Prevent negative snack token balance on order placement
Given a festival goer with 0 snack tokens
When they place an order requiring 1 snack token
Then the order is rejected and the snack token balance remains 0

### Scenario: Prevent negative token balance on transfer
Given festival goer Alice with 1 drink token
And festival goer Bob with 0 drink tokens
When Alice tries to transfer 2 drink tokens to Bob
Then the transfer is rejected and both balances remain unchanged
