# Infrastructure: Transfer Tokens to Another Festival Goer Persistence

**Context**
The Infrastructure module must persist token transfers, track transfer state (PENDING/COMPLETED/REJECTED), manage expiration, and coordinate token balance updates atomically.

**Problem**
The Infrastructure layer requires repositories and transactional handling to persist transfers, update token balances consistently, and dispatch events.

**Acceptance Criteria**
- [ ] `TokenTransferEntity` JPA entity created:
  - Fields: id, senderId, recipientId, drinkTokens, snackTokens, status, createdAt, expiresAt, respondedAt
  - Foreign keys to festival_goers table
  - Index on status for filtering PENDING transfers
  
- [ ] `TokenTransferJpaRepository` Spring Data repository:
  - `findById(String): Optional<TokenTransferEntity>`
  - `save(TokenTransferEntity): TokenTransferEntity`
  - `findAllPendingByRecipientId(String): List<TokenTransferEntity>` (for recipient to see pending)
  
- [ ] `TokenTransferRepositoryAdapter`:
  - Implements Domain port `TokenTransferRepository`
  - Methods: `save`, `findById`, `markAsCompleted`, `markAsRejected`
  
- [ ] Transactional consistency:
  - Token balance updates (both sender and recipient) in same transaction
  - Both succeed or both fail
  
- [ ] Database schema:
  - `token_transfers` table with proper structure
  - Indexes on senderId, recipientId, status
  - Check constraint: drinkTokens <= 3, snackTokens <= 3
  - Foreign keys to festival_goers
  
- [ ] Event publishing:
  - Events published via `EventPublisherAdapter`
  
- [ ] Integration tests verify:
  - Transfer created with PENDING status
  - Confirmation updates both balances atomically
  - Rejection keeps balances unchanged
  - Expiration calculated correctly (24 hours)
  - Query pending transfers for recipient

**Implementation Plan**
1. Create `TokenTransferEntity` JPA entity
2. Create `TokenTransferJpaRepository`
3. Create `TokenTransferRepositoryAdapter`
4. Create database migration for `token_transfers` table
5. Create mappers
6. Create integration tests

**Gherkin Scenarios**
Feature: Transfer Tokens Persistence

Scenario: Create pending transfer successfully
  Given transfer data for sender "s-001" and recipient "r-001"
  And 2 drink tokens, 1 snack token to transfer
  When saved to database
  Then token_transfers record created with status "PENDING"
  And expiresAt = createdAt + 24 hours
  And createdAt timestamp is set

Scenario: Mark transfer as completed
  Given a pending transfer with id "t-001"
  When markAsCompleted is called
  Then status = "COMPLETED"
  And respondedAt timestamp is set to current time

Scenario: Mark transfer as rejected
  Given a pending transfer with id "t-002"
  When markAsRejected is called
  Then status = "REJECTED"
  And respondedAt timestamp is set to current time
  And both token balances remain unchanged

Scenario: Query pending transfers for recipient
  Given multiple transfers: "t-001" PENDING, "t-002" COMPLETED, "t-003" PENDING for recipient "r-001"
  When querying pending transfers for recipient "r-001"
  Then only transfers "t-001" and "t-003" returned
  And COMPLETED transfer "t-002" is not included

Scenario: Query returns empty when no pending transfers
  Given recipient "r-999" has no pending transfers
  When querying pending transfers for recipient "r-999"
  Then empty list is returned
  And no error occurs

Scenario: Confirm transfer updates both balances atomically
  Given pending transfer: sender "s-001" transfers 2 drink tokens to recipient "r-001"
  And sender balance: 5 drink tokens
  And recipient balance: 3 drink tokens
  When confirmation processed in single transaction
  Then sender balance = 3 (5-2)
  And recipient balance = 5 (3+2)
  And transfer marked COMPLETED
  And respondedAt is set

Scenario: Transfer rejection leaves balances unchanged
  Given pending transfer from "s-001" to "r-001" (2 drink tokens)
  And sender balance: 5, recipient balance: 3
  When rejection processed
  Then sender balance remains 5
  And recipient balance remains 3
  And transfer marked REJECTED

Scenario: Cannot mark non-existent transfer as completed (404)
  Given transfer id "t-999" does not exist
  When markAsCompleted is called with "t-999"
  Then TokenTransferNotFoundException is thrown
  And no state changes occur

Scenario: Cannot complete already completed transfer
  Given transfer "t-010" with status "COMPLETED"
  When markAsCompleted is called again
  Then StateConflictException is thrown
  And respondedAt timestamp is not modified

Scenario: Validate token amounts are within constraints
  Given a transfer request with 4 drink tokens (exceeds max 3)
  When saved to database
  Then a ConstraintViolationException is thrown
  Or database constraint is enforced

Scenario: Transfer expiration calculated correctly
  Given transfer created at 2026-06-20 10:00:00
  When transfer record checked
  Then expiresAt = 2026-06-21 10:00:00 (exactly 24 hours later)

Scenario: Query pending transfers with null recipient ID (400)
  Given a null recipient ID
  When querying pending transfers
  Then ValidationException is thrown
  And no database query executed

Scenario: Transaction rollback if balance update fails
  Given pending transfer and constraint violation during update
  When confirmation attempted
  Then entire transaction rolls back
  And transfer status remains PENDING
  And respondedAt remains null

**Notes**
- Transfers expire after 24 hours if not confirmed.
- Atomic updates on token balances.
- Indexes for efficient queries on senderId, recipientId, status.
