# Domain: Stay Hydrated Notifications

**Context**
The festival is held during hot weather, and the bartender wants regular reminders sent to all festival goers to encourage them to drink water. These notifications are sent every hour between 11:00 AM and 7:00 PM. However, if a festival goer has consumed more than 3 alcoholic drinks in the past hour, notifications are sent every 30 minutes instead to encourage hydration and responsible drinking.

**Problem**
The Domain module must provide business logic to determine notification frequency based on festival goer alcohol consumption and trigger hydration reminders at appropriate intervals.

**Acceptance Criteria**
- [ ] `DeterminHydrationNotificationFrequencyService` created:
  - Method: `getNotificationFrequency(festivalGoerId): Duration`
  - Loads festival goer's alcohol consumption from past hour
  - If alcoholConsumed > 3 drinks: return 30 minutes
  - Otherwise: return 60 minutes
  - Returns the appropriate frequency
  
- [ ] `SendHydrationNotificationUseCase` created:
  - Method: `execute(festivalGoerId): void`
  - Loads festival goer
  - Determines appropriate message (standard or increased hydration reminder)
  - Creates notification
  - Publishes `HydrationNotificationSentEvent`
  
- [ ] Scheduled task (infrastructure):
  - Every hour between 11:00 AM and 7:00 PM, iterate all festival goers
  - Determine last notification time
  - If due for notification (based on frequency), trigger it
  
- [ ] Alcohol consumption tracking:
  - Track completed orders with alcoholic drinks
  - Count drinks from past hour
  - Definition: "drink" = individual alcoholic beverage item
  
- [ ] Notification messages:
  - Standard: "Don't forget to drink water! Stay hydrated and enjoy the festival."
  - High consumption: "You've had several drinks! Please remember to drink plenty of water to stay safe and healthy."
  
- [ ] Time window validation:
  - Notifications only sent 11:00-19:00 (7:00 PM)
  - Skip notifications outside this window
  
- [ ] Exception handling:
  - `FestivalGoerNotFoundException`
  - Handle missing last notification timestamp gracefully
  
- [ ] Port definitions in use:
  - `OrderRepository` port: query completed orders for festival goer
  - `NotificationRepository` port (new): `save(Notification)`, `findLastNotificationFor(FestivalGoerId): Optional<Notification>`
  - `EventPublisherPort` port: `publish(HydrationNotificationSentEvent)`

**Implementation Plan**
1. Create `DeterminHydrationNotificationFrequencyService`:
   - Load orders from past 60 minutes
   - Count alcoholic drink items
   - Return Duration(30 min or 60 min)
   
2. Create `SendHydrationNotificationUseCase`:
   - Load festival goer
   - Determine frequency
   - Create notification
   - Publish event
   
3. Create `HydrationNotificationSentEvent` domain event

4. Create `Notification` entity in domain/model (or reference existing)
   - Fields: id, festivalGoerId, message, sentAt, type (STANDARD/HIGH_CONSUMPTION)

5. Define Notification port for persistence

6. Infrastructure scheduler (see application_stay-hydrated.md for details)

**Gherkin Scenarios**
Feature: Stay Hydrated Notifications - Domain Logic

Scenario: Determine 60-minute frequency for low consumption
  Given a festival goer who consumed 1 alcoholic drink in past hour
  When determining notification frequency
  Then frequency is 60 minutes
  
Scenario: Determine 30-minute frequency for high consumption
  Given a festival goer who consumed 5 alcoholic drinks in past hour
  When determining notification frequency
  Then frequency is 30 minutes
  
Scenario: Send standard hydration notification
  Given a festival goer with low alcohol consumption
  When sending notification
  Then message is "Don't forget to drink water!..."
  And HydrationNotificationSentEvent is published
  
Scenario: Send high-consumption hydration reminder
  Given a festival goer with > 3 alcoholic drinks in past hour
  When sending notification
  Then message includes "You've had several drinks!..."
  And event includes HIGH_CONSUMPTION type
  
Scenario: Count only completed orders
  Given orders in states: PENDING, ACKNOWLEDGED, READY, CANCELLED
  When counting drinks from past hour
  Then only READY or completed orders counted (exact definition TBD)
  
Scenario: Count only alcoholic drinks
  Given order with mix of alcoholic and non-alcoholic drinks
  When counting consumption
  Then only alcoholic drinks counted
  
Scenario: Reset count after 1 hour
  Given drinks consumed at time T and time T + 61 minutes
  When querying consumption at time T + 61 minutes
  Then only drink at T + 61 minutes counted (older drink dropped)

Scenario: Boundary - exactly 3 drinks = 60-minute frequency
  Given a festival goer who consumed exactly 3 alcoholic drinks in past hour
  When determining notification frequency
  Then frequency is 60 minutes (NOT 30, since condition is > 3)

Scenario: Boundary - exactly 4 drinks = 30-minute frequency
  Given a festival goer who consumed exactly 4 alcoholic drinks in past hour
  When determining notification frequency
  Then frequency is 30 minutes (>3 threshold met)

Scenario: Festival goer with 0 completed orders in past hour
  Given a festival goer with no orders or all orders > 1 hour old
  When determining notification frequency
  Then consumption count = 0
  And frequency is 60 minutes

Scenario: Non-alcoholic orders do not increment consumption count
  Given completed orders: 1 water, 1 juice, 1 coffee (all non-alcoholic)
  When counting consumption in past hour
  Then consumption count = 0
  And frequency is 60 minutes

Scenario: Mix of alcoholic and non-alcoholic in same hour
  Given 2 alcoholic drinks and 3 non-alcoholic drinks completed in past hour
  When counting consumption
  Then consumption count = 2 (only alcoholic)
  And frequency is 60 minutes

Scenario: Premium drinks count as 1 drink for consumption (despite costing 2 tokens)
  Given 1 regular beer (costs 1 token) and 1 premium beer (costs 2 tokens) in past hour
  When counting consumption
  Then consumption count = 2 (both count as drinks)
  And frequency is 30 minutes (> 3 = false, but count = 2... wait this should be 60 min)

Scenario: Frequency determined correctly at midnight edge
  Given current time is 23:59 (11:59 PM)
  And orders completed at 23:30 (past hour from 22:59-23:59)
  When determining frequency
  Then consumption is calculated correctly
  And frequency returned

**Notes**
- Frequency is dynamic: updates per notification based on latest hour consumption.
- Time window is 11:00-19:00 daily.
- Tracks only *completed* orders (exact state TBD with user).
- Alcoholic drinks: normal (1 drink) and premium (counts as 1 drink for consumption, even though costs 2 tokens).
