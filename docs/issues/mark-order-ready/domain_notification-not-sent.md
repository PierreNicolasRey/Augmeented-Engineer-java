# Issue: Domain notification not sent when order is marked ready

**Context**
When a bartender marks an order as ready, the system must notify the festival goer so they can pick up their order.

**Problem**
In the current domain flow, the order readiness event is not translated into a notification trigger, so the festival goer is never informed that the order is ready.

**Acceptance Criteria**
- When the bartender marks an acknowledged order as ready, the system publishes an order ready domain event.
- The published event must contain the festival goer identifier and order readiness details.
- If order readiness cannot be confirmed, the operation is rejected and no ready notification is published.

**Implementation Plan**
1. Verify the domain use case for marking orders as ready publishes the proper domain event.
2. Ensure the event carries the festival goer identifier and order readiness details.
3. Add domain tests to assert that marking ready results in the notification event being published.
4. Confirm the triggering event is handled by the notification pipeline.

**Gherkin Scenarios**
Feature: Notify festival goer when order is marked ready

Scenario: Send notification after order readiness is confirmed
Given an order that has been acknowledged and is ready for pickup
When the bartender marks the order as ready
Then the system publishes an order ready event and the festival goer receives a notification

Scenario: Do not send notification if order readiness cannot be confirmed
Given an order whose readiness validation fails
When the bartender attempts to mark it ready
Then the operation is rejected and no ready notification is sent

**Notes**
- This issue focuses on the domain event and notification trigger, not on the transport mechanism.