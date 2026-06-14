# Issue: Infrastructure fails to persist order ready flag

**Context**
The bartender marks an order as ready, and the system must persist the ready state so the order can move to pickup and the festival goer can be notified.

**Problem**
The persistence layer is failing to update the order record with `isReady = true`, preventing the order from reaching the ready state in the database.

**Acceptance Criteria**
- When the bartender marks an acknowledged order as ready, the order record is updated with `isReady = true` in persistence.
- The persistence layer must save the ready state reliably for pickup processing.
- If persisting `isReady = true` fails, the operation reports failure and the order remains not ready.

**Implementation Plan**
1. Review the persistence adapter handling the order ready update.
2. Fix mapping or repository usage that prevents `isReady` from being saved.
3. Add integration tests to verify the ready flag persists correctly.
4. Ensure failures are surfaced cleanly to the domain use case.

**Gherkin Scenarios**
Feature: Persist order ready state in infrastructure

Scenario: Update order ready flag successfully
Given an acknowledged order that is ready for pickup
When the order ready state is persisted
Then the order record is updated with `isReady = true`

Scenario: Reject ready state update on database failure
Given an acknowledged order ready for pickup
When persisting `isReady = true` fails
Then the operation reports failure and the order remains not ready

**Notes**
- This issue focuses on persistence and mapping; notification behavior is handled by the domain event flow.