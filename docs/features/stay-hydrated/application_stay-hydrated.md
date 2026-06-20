# Application: Stay Hydrated Notifications

**Context**
Festival goers receive hydration reminder notifications on their mobile app or via push notifications/email. These notifications are scheduled and triggered automatically by the system on a regular basis throughout the day.

**Problem**
The Application layer must provide notification handling infrastructure to dispatch hydration reminders to festival goers through appropriate channels (push notification, email, SMS, or in-app message).

**Acceptance Criteria**
- [ ] Notification dispatcher created:
  - Method: `sendNotification(festivalGoerId, message, type): void`
  - Determines delivery channel (push, email, SMS, in-app)
  - Formats message appropriately for channel
  - Sends notification
  
- [ ] Scheduled task created:
  - Runs every hour between 11:00 and 19:00
  - Queries all festival goers
  - For each goer: check if notification due (based on last notification and frequency)
  - If due: call `SendHydrationNotificationUseCase.execute(festivalGoerId)`
  
- [ ] Notification channels supported:
  - Push notification (primary, for mobile app)
  - Email (fallback)
  - In-app message (stored in database)
  
- [ ] Festival goer notification preferences:
  - Can opt-out of notifications (optional feature)
  - Stored in festival goer profile
  
- [ ] Scheduler configuration:
  - Fixed delay or cron expression: `0 * 11-18 * * *` (hourly 11 AM to 6 PM, runs at top of hour)
  - Configurable via application properties
  - Can be disabled for testing
  
- [ ] Error handling:
  - Notification send failure does not block other goers
  - Log failures for monitoring
  - Retry logic (optional)
  
- [ ] Notification preview/testing:
  - HTTP endpoint to manually trigger notification (for testing)
  - Method: `POST /api/v1/notifications/hydration-test`
  - Sends test notification to authenticated festival goer

**Implementation Plan**
1. Create `NotificationDispatcher` service:
   - Methods: `sendPushNotification()`, `sendEmailNotification()`, `sendInAppMessage()`
   - Routes to appropriate channel based on preference
   
2. Create `HydrationNotificationScheduler`:
   - `@Scheduled` task with cron expression
   - Runs hourly between 11-19:00
   - Iterates all active festival goers
   - Checks if due for notification
   - Calls use case
   
3. Create `HydrationNotificationController` (optional):
   - POST endpoint for manual testing
   - `POST /api/v1/notifications/hydration-test`
   - Returns confirmation
   
4. Create `NotificationPreference` entity (optional):
   - Fields: festival goer ID, opt-in/out, preferred channels
   
5. Application properties:
   ```yaml
   hydration-notification:
     enabled: true
     schedule-cron: "0 * 11-18 * * *"
     time-window-start: 11:00
     time-window-end: 19:00
     channels:
       push: true
       email: true
       in-app: true
   ```

**Gherkin Scenarios**
Feature: Stay Hydrated Notifications - Delivery

Scenario: Push notification sent at scheduled time
  Given scheduler time is 12:00 (within 11-19 window)
  And festival goer is due for notification
  When scheduled task runs
  Then push notification is sent to festival goer's mobile app
  And in-app message is also created
  And last-notification timestamp is updated

Scenario: Notifications skipped outside time window
  Given scheduler time is 08:00 (before 11:00)
  When scheduled task runs
  Then no notifications are sent
  
Scenario: Notifications skipped after 19:00
  Given scheduler time is 20:00 (after 19:00)
  When scheduled task runs
  Then no notifications are sent
  
Scenario: High-consumption notification content
  Given festival goer with > 3 drinks in past hour
  When notification is generated and sent
  Then message content includes "You've had several drinks!"
  And push notification emphasizes hydration
  
Scenario: Standard notification content
  Given festival goer with <= 3 drinks in past hour
  When notification is generated and sent
  Then message content is standard reminder
  And push notification is friendly tone
  
Scenario: Notification frequency: low consumption (hourly)
  Given festival goer with low alcohol consumption
  And last notification sent at 12:00
  When scheduler runs at 13:00
  Then notification is sent (60-minute interval)
  
Scenario: Notification frequency: high consumption (every 30 minutes)
  Given festival goer with > 3 drinks in past hour
  And last notification sent at 12:00
  When scheduler runs at 12:30
  Then notification is sent (30-minute interval)
  When scheduler runs at 13:00
  Then notification is sent again (30-minute interval)
  
Scenario: Manual test notification
  Given a festival goer is authenticated
  When a POST request is sent to `/api/v1/notifications/hydration-test`
  Then HTTP 200 OK is returned
  And test notification is sent immediately
  And response confirms delivery
  
Scenario: Opt-out of notifications
  Given a festival goer has opt-out preference
  When scheduler tries to send notification
  Then notification is skipped
  And no message is sent
  
Scenario: Fallback to email if push unavailable
  Given push notification service is unavailable
  When trying to send notification
  Then fallback to email is attempted
  And email notification is sent instead

Scenario: Scheduler skips notifications before 11:00 AM
  Given current time is 10:59 AM
  When the hydration scheduler runs
  Then no notifications are sent
  And scheduler completes successfully

Scenario: Scheduler starts notifications at 11:00 AM exactly
  Given current time is 11:00:00 AM (exactly)
  And festival goers due for notification
  When the scheduler runs
  Then notifications are sent
  And all eligible festival goers receive message

Scenario: Scheduler skips notifications at 7:00 PM (19:00) - boundary test
  Given current time is 19:00:00 (7:00 PM exactly)
  And festival goers due for notification
  When the scheduler runs
  Then notifications MAY be sent (clarify: is 19:00 included or excluded?)
  And behavior is documented

Scenario: Scheduler skips notifications after 7:00 PM (19:01)
  Given current time is 19:01 (after 7 PM window ends)
  When the scheduler runs
  Then no notifications are sent
  And scheduler completes without error

Scenario: Window transition: last notification before 7 PM, then no more after
  Given last notification sent at 18:30 (6:30 PM)
  And festival goer is due for another notification at 19:30 (7:30 PM)
  When scheduler runs at 19:30
  Then notification is NOT sent
  And no error occurs (window has closed)

Scenario: Midnight-crossing notifications
  Given last notification sent at 23:00 (11 PM, outside window - shouldn't happen)
  And current time is 00:00 (midnight)
  When scheduler runs
  Then no notifications sent
  And window reset happens for new day (at 11:00 AM)

**Notes**
- Scheduled task runs hourly at top of hour.
- Only sends within 11:00-19:00 daily window.
- Frequency dynamically updated per notification (not cached).
- Supports opt-out via user preferences.
- Multiple delivery channels for resilience.
- Manual test endpoint for QA/testing.
