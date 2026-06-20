# Infrastructure: Stay Hydrated Notifications Persistence

**Context**
The Infrastructure module must handle persistent storage of notifications, schedule recurring tasks, manage notification delivery channels, and maintain notification history and user preferences.

**Problem**
The Infrastructure layer requires repositories for notifications and preferences, Spring scheduling configuration, and adapters for external notification services (push/email/SMS).

**Acceptance Criteria**
- [ ] `NotificationEntity` JPA entity created:
  - Fields: id, festivalGoerId, message, type (STANDARD/HIGH_CONSUMPTION), sentAt, deliveryStatus (PENDING/SENT/FAILED/FAILED_AND_RETRIED)
  - Foreign key to festival_goers
  - Index on (festivalGoerId, sentAt)
  
- [ ] `NotificationPreferenceEntity` JPA entity created:
  - Fields: id, festivalGoerId, opt-in (boolean), preferredChannels (push, email, sms)
  - Foreign key to festival_goers
  
- [ ] `NotificationJpaRepository` Spring Data:
  - `save(NotificationEntity): NotificationEntity`
  - `findLastNotificationFor(festivalGoerId): Optional<NotificationEntity>`
  - `findAllForFestivalGoer(festivalGoerId): List<NotificationEntity>` (with pagination)
  
- [ ] `NotificationPreferenceJpaRepository` Spring Data:
  - `findByFestivalGoerId(festivalGoerId): Optional<NotificationPreferenceEntity>`
  - `save(NotificationPreferenceEntity): NotificationPreferenceEntity`
  
- [ ] `NotificationRepositoryAdapter`:
  - Implements Domain port `NotificationRepository`
  - Methods: `save`, `findLastNotificationFor`
  
- [ ] Push notification service adapter:
  - Interface: `PushNotificationService`
  - Implementation for Firebase Cloud Messaging (FCM) or similar
  - Method: `sendPushNotification(deviceToken, title, message): boolean`
  
- [ ] Email notification service adapter:
  - Interface: `EmailNotificationService`
  - Implementation using Spring Mail or external service (SendGrid, etc.)
  - Method: `sendEmailNotification(email, subject, message): boolean`
  
- [ ] `HydrationNotificationScheduler` Spring component:
  - `@Scheduled(cron = "0 * 11-18 * * *")`
  - Queries all festival goers
  - Loads preferences and last notification
  - Calls `SendHydrationNotificationUseCase` if due
  - Handles errors gracefully
  
- [ ] Database schema:
  - `notifications` table with proper structure
  - `notification_preferences` table
  - Indexes on festivalGoerId, sentAt, deliveryStatus
  
- [ ] Configuration properties:
  ```yaml
  notification:
    fcm:
      project-id: ${FCM_PROJECT_ID}
      private-key: ${FCM_PRIVATE_KEY}
    email:
      from: notifications@festival.fr
      smtp-host: ${SMTP_HOST}
  ```
  
- [ ] Integration tests verify:
  - Notification saved to database
  - Last notification query works correctly
  - Preferences honored (opt-in/out, channels)
  - Scheduler runs at correct times
  - Multiple notifications sent to multiple goers

**Implementation Plan**
1. Create `NotificationEntity` JPA entity
2. Create `NotificationPreferenceEntity` JPA entity
3. Create `NotificationJpaRepository`
4. Create `NotificationPreferenceJpaRepository`
5. Create `NotificationRepositoryAdapter`
6. Create `PushNotificationServiceAdapter`
7. Create `EmailNotificationServiceAdapter`
8. Create `HydrationNotificationScheduler`
9. Create database migrations
10. Create integration tests

**Gherkin Scenarios**
Feature: Stay Hydrated Notifications - Persistence & Scheduling

Scenario: Notification persisted to database
  Given a hydration notification for festival goer
  When saved via repository
  Then notification record created in database
  And sentAt timestamp set
  And deliveryStatus = PENDING
  
Scenario: Retrieve last notification
  Given multiple notifications for festival goer
  When querying last notification
  Then most recent notification returned
  
Scenario: Query notifications with pagination
  Given 50 notifications for festival goer
  When querying with page size 20
  Then first 20 results returned
  And pagination info available
  
Scenario: Scheduled task runs hourly
  Given scheduler configured with cron "0 * 11-18 * * *"
  When current time is 12:00
  Then scheduler task executes
  And all active festival goers checked
  And due notifications sent
  
Scenario: Notification preference: opt-out
  Given festival goer with opt-in = false
  When scheduler checks preferences
  Then notification is skipped
  And no message sent
  
Scenario: Notification preference: channel selection
  Given festival goer with preferred channels = [push, email]
  When sending notification
  Then push notification sent
  And email notification sent
  And SMS not sent
  
Scenario: Push notification sent via FCM
  Given festival goer with device token
  When push notification sent
  Then FCM API called with correct message
  And deliveryStatus updated to SENT
  
Scenario: Email notification sent
  Given festival goer email address
  When email notification sent
  Then email sent to festival goer
  And deliveryStatus updated to SENT
  
Scenario: Failed notification retry
  Given notification send fails first time
  When retry logic triggered
  Then deliveryStatus = FAILED_AND_RETRIED
  And notification retried up to max attempts
  
Scenario: Scheduler continues on error
  Given notification send fails for festival goer #1
  When processing multiple festival goers
  Then goer #1 failure logged
  And scheduler continues to goer #2
  And goer #2 notification sent successfully

Scenario: Scheduler skips execution before 11:00 AM
  Given scheduled task configured for 10:59 AM (before window)
  When the Quartz/scheduling system triggers
  Then task checks time window
  And no notifications are attempted
  And task logs that window is closed

Scenario: Scheduler executes at 11:00 AM window start
  Given scheduled task triggered at 11:00:00 AM
  And multiple festival goers have notifications queued
  When the scheduler runs
  Then all eligible goers receive notifications
  And scheduler completes successfully

Scenario: Scheduler skips execution at 19:01 (after 7 PM window)
  Given scheduled task configured for 19:01 (7:01 PM)
  When the scheduler is triggered
  Then task checks time window
  And no notifications are sent
  And task completes without error

Scenario: Scheduler handles window boundary at exactly 19:00 (7 PM)
  Given scheduled task triggered at 19:00:00 (7:00 PM exactly)
  When the scheduler runs
  Then behavior is consistent with specification:
    - Either notifications ARE sent (7 PM included), or
    - Notifications are SKIPPED (7 PM excluded)
  And behavior is tested and documented

Scenario: Daily window reset: new day starts at 11:00 AM
  Given notifications sent throughout 11 AM-7 PM on Day 1
  When Day 2 arrives at 10:59 AM
  Then no notifications sent (window hasn't opened)
  When Day 2 reaches 11:00 AM
  Then notifications resume for new day
  And last_notification_time is properly managed

Scenario: Concurrent scheduler runs during same time window
  Given scheduler is configured to run every minute
  And 11:00 AM window
  When two scheduler instances trigger simultaneously
  Then only one set of notifications is sent
  Or deduplication prevents duplicate messages
  And database locking prevents race conditions

**Notes**
- Notifications stored for audit/history.
- Delivery status tracked: PENDING → SENT/FAILED/FAILED_AND_RETRIED.
- Preferences allow opt-out and channel selection.
- Scheduler runs at top of each hour, 11 AM to 6 PM.
- Failure handling: log and continue (don't block other goers).
- External services (FCM, email) configured via environment variables for security.
