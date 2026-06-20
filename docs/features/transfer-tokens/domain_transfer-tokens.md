# Domain: Transfer Tokens to Another Festival Goer

**Context**
Festival goers may wish to share tokens with friends by transferring up to 3 tokens of each type to another festival goer. The transfer must be confirmed by the recipient to prevent unwanted transfers. A festival goer cannot transfer tokens that would result in a negative balance.

**Problem**
The Domain module must provide business logic to initiate token transfers and confirm them once accepted by the recipient. Transfers must validate token availability and maintain balance integrity.

**Acceptance Criteria**
- [ ] `InitiateTokenTransferUseCase` created:
  - Method: `execute(senderGoerId, recipientGoerId, drinkTokens, snackTokens): TransferId`
  - Loads sender's token balance
  - Validates sender has sufficient tokens
  - Validates transfer amounts <= 3 tokens of each type
  - Creates pending transfer request
  - Publishes `TokenTransferInitiatedEvent`
  - Returns transfer ID
  
- [ ] `ConfirmTokenTransferUseCase` created:
  - Method: `execute(transferId, recipientGoerId): void`
  - Loads pending transfer request
  - Validates transfer still PENDING (not expired, not rejected)
  - Loads both sender and recipient token balances
  - Decrements sender tokens, increments recipient tokens
  - Marks transfer as COMPLETED
  - Publishes `TokenTransferCompletedEvent`
  
- [ ] `RejectTokenTransferUseCase` created:
  - Method: `execute(transferId, recipientGoerId): void`
  - Marks transfer as REJECTED
  - Does not modify token balances
  - Publishes `TokenTransferRejectedEvent`
  
- [ ] Validation:
  - Sender must have sufficient tokens
  - Transfer amounts: 0 <= amount <= 3 for each type
  - Sender and recipient must be different
  - Sender cannot transfer to themselves
  - Recipient exists before transfer created
  
- [ ] Token balance consistency:
  - On confirmation: sender tokens decrease, recipient tokens increase
  - Both transfers atomic (both succeed or both fail)
  
- [ ] Exception handling:
  - `FestivalGoerNotFoundException` if sender or recipient not found
  - `InsufficientTokensException` if sender doesn't have enough tokens
  - `InvalidTransferAmountException` if transfer > 3 tokens
  - `TransferCannotBeConfirmedException` if transfer not PENDING or expired
  - `SelfTransferNotAllowedException` if sender == recipient
  
- [ ] Port definitions in use:
  - `TokenBalanceRepository` port: `findTokenBalanceByFestivalGoerId`, `saveTokenBalance`
  - `TokenTransferRepository` port (new): `save`, `findById`, `markAsCompleted`, `markAsRejected`
  - `EventPublisherPort` port: `publish(events)`

**Implementation Plan**
1. Create `TokenTransfer` entity in domain/model:
   - Fields: id, senderId, recipientId, drinkTokens, snackTokens, status (PENDING/COMPLETED/REJECTED), createdAt, expiresAt (24 hours), respondedAt
   
2. Create `InitiateTokenTransferUseCase`:
   - Validate sender has tokens
   - Validate transfer amounts <= 3
   - Create pending transfer
   - Publish event
   - Return transfer ID
   
3. Create `ConfirmTokenTransferUseCase`:
   - Load transfer, validate PENDING
   - Load both balances
   - Decrement sender, increment recipient
   - Save both balances
   - Mark transfer completed
   - Publish event
   
4. Create `RejectTokenTransferUseCase`:
   - Load transfer
   - Mark rejected
   - Publish event

5. Define exceptions

6. Create domain events:
   - `TokenTransferInitiatedEvent`
   - `TokenTransferCompletedEvent`
   - `TokenTransferRejectedEvent`

**Gherkin Scenarios**
Feature: Transfer Tokens to Another Festival Goer

Scenario: Initiate token transfer with sufficient tokens
  Given a festival goer "fgv-001" with 6 drink tokens
  And a festival goer "fgv-002" with 3 drink tokens
  When initiating transfer of 2 drink tokens to "fgv-002"
  Then transfer status is "PENDING"
  And transfer ID is returned
  And TokenTransferInitiatedEvent is published
  And "fgv-001" balance unchanged (tokens not yet transferred)

Scenario: Confirm token transfer
  Given a pending transfer "tf-001" of 2 drink tokens from "fgv-001" to "fgv-002"
  When "fgv-002" confirms the transfer
  Then transfer status changes to "COMPLETED"
  And "fgv-001" balance: 6 - 2 = 4 drink tokens
  And "fgv-002" balance: 3 + 2 = 5 drink tokens
  And TokenTransferCompletedEvent is published

Scenario: Reject token transfer
  Given a pending transfer "tf-002"
  When "fgv-002" rejects the transfer
  Then transfer status changes to "REJECTED"
  And both balances remain unchanged

Scenario: Cannot transfer more than 3 tokens of one type
  Given attempting to transfer 4 drink tokens
  When initiating the transfer
  Then InvalidTransferAmountException is raised

Scenario: Cannot transfer with insufficient tokens
  Given a festival goer with 1 drink token
  When attempting to transfer 2 drink tokens
  Then InsufficientTokensException is raised

Scenario: Cannot transfer to self
  Given attempting to transfer tokens to same festival goer
  When initiating the transfer
  Then SelfTransferNotAllowedException is raised

Scenario: Mixed token transfer
  Given initiating transfer of 2 drink tokens and 1 snack token
  When confirmed
  Then sender decremented by both amounts
  And recipient incremented by both amounts

Scenario: Transfer expires after 24 hours (pending)
  Given a transfer created 25 hours ago, still PENDING
  When attempting to confirm
  Then TransferCannotBeConfirmedException is raised (expired)

Scenario: Festival goer not found
  Given transfer to non-existent festival goer
  When initiating the transfer
  Then FestivalGoerNotFoundException is raised

**Notes**
- Transfers are PENDING until confirmed or rejected by recipient.
- Limited to 3 tokens of each type per transfer (user requirement).
- Transfers expire after 24 hours if not confirmed/rejected.
- Token balances not modified until transfer confirmed.
