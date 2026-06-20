# Application: Transfer Tokens to Another Festival Goer

**Context**
Festival goers need HTTP endpoints to initiate token transfers, confirm received transfers, and reject unwanted transfers.

**Problem**
The Application layer must provide RESTful interfaces for token transfer workflow: initiate, confirm, and reject.

**Acceptance Criteria**
- [ ] HTTP endpoint for initiation created:
  - Method: `POST /api/v1/tokens/transfer`
  - Request body: `InitiateTransferRequestDTO` with recipientGoerId, drinkTokens, snackTokens
  - Response: HTTP 201 Created with `TransferInitiatedResponseDTO`
  
- [ ] HTTP endpoint for confirmation created:
  - Method: `POST /api/v1/tokens/transfers/{transferId}/confirm`
  - No request body
  - Response: HTTP 200 OK with `TransferConfirmedResponseDTO`
  
- [ ] HTTP endpoint for rejection created:
  - Method: `POST /api/v1/tokens/transfers/{transferId}/reject`
  - No request body
  - Response: HTTP 200 OK with `TransferRejectedResponseDTO`
  
- [ ] `InitiateTransferRequestDTO`:
  - Field: `recipientGoerId: String` (required)
  - Field: `drinkTokens: int` (0-3, required)
  - Field: `snackTokens: int` (0-3, required)
  - Validation: both > 0 or at least one > 0
  
- [ ] `TransferInitiatedResponseDTO`:
  - Field: `transferId: String`
  - Field: `status: String` ("PENDING")
  - Field: `senderId: String`
  - Field: `recipientId: String`
  - Field: `drinkTokens: int`
  - Field: `snackTokens: int`
  - Field: `expiresAt: Instant`
  - Field: `message: String`
  
- [ ] `TransferConfirmedResponseDTO`:
  - Field: `transferId: String`
  - Field: `status: String` ("COMPLETED")
  - Field: `message: String`
  - Field: `newBalance: TokenBalanceDTO` (updated balance for recipient)
  
- [ ] `TransferRejectedResponseDTO`:
  - Field: `transferId: String`
  - Field: `status: String` ("REJECTED")
  - Field: `message: String`
  
- [ ] Controller methods:
  - `initiateTransfer(initiateDtoDTO): ResponseEntity<?>` (sender implicitly from auth)
  - `confirmTransfer(transferId): ResponseEntity<?>`
  - `rejectTransfer(transferId): ResponseEntity<?>`
  
- [ ] Error responses:
  - HTTP 400: Invalid transfer amounts (> 3)
  - HTTP 400: Insufficient tokens
  - HTTP 400: Self-transfer
  - HTTP 404: Recipient not found
  - HTTP 404: Transfer not found
  - HTTP 409: Transfer not PENDING
  - HTTP 410: Transfer expired
  
- [ ] Mappers created

**Implementation Plan**
1. Create DTOs
2. Add controller methods to new `TokenTransferController` (or existing controller)
3. Create mappers

**Gherkin Scenarios**
Feature: Token Transfer REST Endpoints

Scenario: Initiate transfer (HTTP 201)
  Given a festival goer is authenticated
  And recipient exists
  When a POST request is sent to `/api/v1/tokens/transfer` with valid transfer data
  Then HTTP 201 Created is returned
  And response includes transferId, status "PENDING", expiresAt
  
Scenario: Confirm transfer (HTTP 200)
  Given a pending transfer "tf-001"
  And recipient is authenticated
  When a POST request is sent to `/api/v1/tokens/transfers/tf-001/confirm`
  Then HTTP 200 OK is returned
  And status changes to "COMPLETED"
  And newBalance shows updated tokens
  
Scenario: Reject transfer (HTTP 200)
  Given a pending transfer "tf-002"
  When a POST request is sent to `/api/v1/tokens/transfers/tf-002/reject`
  Then HTTP 200 OK is returned
  And status changes to "REJECTED"
  
Scenario: Cannot transfer > 3 tokens (HTTP 400)
  Given a festival goer with sufficient balance
  When initiating transfer of 4 tokens
  Then HTTP 400 Bad Request is returned
  
Scenario: Insufficient tokens (HTTP 400)
  Given a festival goer with insufficient balance
  When initiating transfer with all remaining balance
  Then HTTP 400 Bad Request is returned
  
Scenario: Transfer expired (HTTP 410)
  Given a transfer created 25 hours ago
  When attempting to confirm
  Then HTTP 410 Gone is returned

**Notes**
- Sender is implicitly from authentication context.
- Recipient must exist before transfer initiated.
- Transfers limited to 3 tokens each type.
