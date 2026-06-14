# Issue: Prevent negative drink and snack token balances

**Context**
The feature for consulting a festival goer's remaining tokens relies on a domain invariant: drink and snack balances must never be negative.

**Problem**
A festival goer may end up with a negative token balance because domain-level validation is missing when balances are updated during transfers, order placement, or refunds.

**Acceptance Criteria**
- The domain model must enforce that drink and snack token balances are never negative.
- All token operations must reject adjustments that would cause a negative balance.
- The invariant must be implemented in the domain layer and covered by focused domain tests.

**Implementation Plan**
1. Identify the domain model or aggregate responsible for festival goer token balances.
2. Add domain validation preventing token balance adjustments that would result in negative drink or snack balances.
3. Add unit tests covering:
   - invalid direct balance adjustments to negative values
   - transfers that would cause a negative balance
   - order placement that exceeds available tokens
   - cancellation/refund behavior that maintains non-negative balances
4. Verify that persistence adapters and mappers do not allow invalid domain state to be stored.

**Gherkin Scenarios**
Feature: Prevent negative token balances in festival goer token accounting

Scenario: Prevent negative drink token balance on issuance
Given a festival goer with 0 drink tokens
When the system attempts to reduce their drink tokens by 1
Then the operation is rejected and the drink token balance remains 0

Scenario: Prevent negative snack token balance on order placement
Given a festival goer with 0 snack tokens
When they place an order requiring 1 snack token
Then the order is rejected and the snack token balance remains 0

Scenario: Prevent negative token balance on transfer
Given festival goer Alice with 1 drink token
And festival goer Bob with 0 drink tokens
When Alice tries to transfer 2 drink tokens to Bob
Then the transfer is rejected and both balances remain unchanged
