# Application: Approve or Reject Order Changes

**Context**
Bartenders need HTTP endpoints to approve or reject change requests from festival goers for acknowledged orders. The endpoints must allow the bartender to review and respond to change requests with decisions.

**Problem**
The Application layer must provide RESTful interfaces for bartenders to approve or reject order changes, mapping HTTP requests to domain commands and returning appropriate responses.

**Acceptance Criteria**
- [ ] HTTP endpoint for approval created:
  - Method: `POST /api/v1/orders/{orderId}/changes/approve`
  - Path parameter: `orderId` (string, required)
  - No request body (or metadata)
  
- [ ] HTTP endpoint for rejection created:
  - Method: `POST /api/v1/orders/{orderId}/changes/reject`
  - Path parameter: `orderId` (string, required)
  - Request body: `RejectChangeRequestDTO` with reason
  
- [ ] `ApproveChangeResponseDTO` structure:
  - Field: `orderId: String`
  - Field: `status: String` ("ACKNOWLEDGED")
  - Field: `newEstimatedReadinessAt: Instant`
  - Field: `newEstimatedReadinessMinutes: int`
  - Field: `message: String` (e.g., "Change approved")
  - Field: `approvedAt: Instant`
  
- [ ] `RejectChangeRequestDTO` structure:
  - Field: `reason: String` (reason for rejection)
  - Validation: reason non-empty
  
- [ ] `RejectChangeResponseDTO` structure:
  - Field: `orderId: String`
  - Field: `status: String` ("ACKNOWLEDGED")
  - Field: `message: String` (e.g., "Change rejected")
  - Field: `reason: String` (echoed back)
  - Field: `rejectedAt: Instant`
  
- [ ] Controller methods:
  - `approveOrderChange(orderId): ResponseEntity<?>`
  - `rejectOrderChange(orderId, rejectChangeRequestDTO): ResponseEntity<?>`
  - Both handle exceptions and return appropriate HTTP status codes
  
- [ ] Success response (HTTP 200):
  - Approval: `ApproveChangeResponseDTO` with new estimated time
  - Rejection: `RejectChangeResponseDTO` with reason
  
- [ ] Error response HTTP 404 (Not Found):
  - Scenario: Order or change request not found
  - Response: `{ "error": "ORDER_NOT_FOUND", "message": "Order {orderId} not found" }`
  
- [ ] Error response HTTP 409 (Conflict):
  - Scenario: Cannot approve (no transferable items)
  - Response: `{ "error": "CANNOT_APPROVE_CHANGE", "message": "No prepared items available to transfer. Cannot approve changes" }`
  
- [ ] Error response HTTP 409 (Conflict):
  - Scenario: Order not in ACKNOWLEDGED state
  - Response: `{ "error": "ORDER_NOT_ACKNOWLEDGED", "message": "Only ACKNOWLEDGED orders can have changes approved or rejected" }`
  
- [ ] Error response HTTP 400 (Bad Request):
  - Scenario: Invalid rejection reason (empty)
  - Response: `{ "error": "INVALID_REASON", "message": "Rejection reason must be provided" }`
  
- [ ] Request/Response DTOs created:
  - `ApproveChangeResponseDTO` in application/dto/response
  - `RejectChangeRequestDTO` in application/dto/request
  - `RejectChangeResponseDTO` in application/dto/response
  
- [ ] Mappers created:
  - `ApproveChangeResponseMapper` in application/mapper
  - `RejectChangeResponseMapper` in application/mapper
  
- [ ] Controller method routing:
  - Endpoints: `POST /api/v1/orders/{orderId}/changes/approve` and `/reject`
  - Status codes: 200, 400, 404, 409

- [ ] `OrderQueryService` injected in controller:
  - Used to fetch updated order after use case execution
  - Method: `findOrderById(orderId): Optional<Order>` (CQS pattern, not repository)

**Implementation Plan**
1. Create DTOs:
   
   - `RejectChangeRequestDTO`:
     ```java
     public record RejectChangeRequestDTO(
       @NotBlank(message = "Reason must be provided")
       String reason
     ) {}
     ```
   
   - `ApproveChangeResponseDTO`:
     ```java
     public record ApproveChangeResponseDTO(
       String orderId,
       String status,
       Instant newEstimatedReadinessAt,
       int newEstimatedReadinessMinutes,
       String message,
       Instant approvedAt
     ) {}
     ```
   
   - `RejectChangeResponseDTO`:
     ```java
     public record RejectChangeResponseDTO(
       String orderId,
       String status,
       String message,
       String reason,
       Instant rejectedAt
     ) {}
     ```

2. Create mappers:
   
   - `ApproveChangeResponseMapper`:
     ```java
     public ApproveChangeResponseDTO toDTO(Order order) {
       // NOTE: No logic here, only data transformation
       // estimatedReadinessMinutes is pre-calculated in domain (Order.getEstimatedReadinessMinutes())
       // approvedAt is NOT calculated here - it comes from domain event or use case
       return new ApproveChangeResponseDTO(
         order.getId(),
         "ACKNOWLEDGED",
         order.getEstimatedReadinessAt(),
         order.getEstimatedReadinessMinutes(), // Use pre-calculated value from domain
         "Change approved",
         order.getApprovedAt() // Or pass via method parameter from use case
       );
     }
     ```
   
   - `RejectChangeResponseMapper`:
     ```java
     public RejectChangeResponseDTO toDTO(String orderId, String reason, Instant rejectedAt) {
       // NOTE: rejectedAt is passed in, not calculated by mapper
       return new RejectChangeResponseDTO(
         orderId,
         "ACKNOWLEDGED",
         "Change rejected",
         reason,
         rejectedAt // Passed from domain/use case, not Instant.now()
       );
     }
     ```

3. Add controller methods:
   
   ```java
   @PostMapping("/{orderId}/changes/approve")
   public ResponseEntity<?> approveOrderChange(
     @PathVariable String orderId
   ) {
     try {
       // Call use case
       approveOrderChangeUseCase.execute(orderId);
       
       // Fetch updated order via query service (CQS pattern)
       Order updatedOrder = orderQueryService.findOrderById(orderId)
         .orElseThrow(() -> new OrderNotFoundException(orderId));
       
       return ResponseEntity.ok(approveChangeResponseMapper.toDTO(updatedOrder));
     } catch (OrderNotFoundException ex) {
       return ResponseEntity.notFound().build();
     } catch (OrderChangeRequestNotFoundException ex) {
       return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
         ErrorResponse.of("CHANGE_REQUEST_NOT_FOUND", ex.getMessage())
       );
     } catch (CannotApproveChangeException ex) {
       return ResponseEntity.status(HttpStatus.CONFLICT).body(
         ErrorResponse.of("CANNOT_APPROVE_CHANGE", ex.getMessage())
       );
     }
   }
   
   @PostMapping("/{orderId}/changes/reject")
   public ResponseEntity<?> rejectOrderChange(
     @PathVariable String orderId,
     @RequestBody RejectChangeRequestDTO request
   ) {
     try {
       approveOrderChangeUseCase.execute(orderId, getBartenderId());
       Order approvedOrder = orderRepository.findOrderById(orderId).orElseThrow();
       return ResponseEntity.ok(approprveChangeResponseMapper.toDTO(approvedOrder));
     } catch (OrderNotFoundException ex) {
       return ResponseEntity.notFound().build();
     } catch (CannotApproveChangeException ex) {
       return ResponseEntity.status(HttpStatus.CONFLICT).body(
         ErrorResponse.of("CANNOT_APPROVE_CHANGE", ex.getMessage())
       );
     } catch (OrderNotInAcknowledgedStateException ex) {
       return ResponseEntity.status(HttpStatus.CONFLICT).body(
         ErrorResponse.of("ORDER_NOT_ACKNOWLEDGED", ex.getMessage())
       );
     }
   }
   
   @PostMapping("/{orderId}/changes/reject")
   public ResponseEntity<?> rejectOrderChange(
     @PathVariable String orderId,
     @Valid @RequestBody RejectChangeRequestDTO request
   ) {
     try {
       if (request.reason() == null || request.reason().isBlank()) {
         return ResponseEntity.badRequest().body(
           ErrorResponse.of("INVALID_REASON", "Rejection reason must be provided")
         );
       }
       
       rejectOrderChangeUseCase.execute(orderId, request.reason(), getBartenderId());
       return ResponseEntity.ok(rejectChangeResponseMapper.toDTO(orderId, request.reason()));
     } catch (OrderNotFoundException ex) {
       return ResponseEntity.notFound().build();
     } catch (OrderNotInAcknowledgedStateException ex) {
       return ResponseEntity.status(HttpStatus.CONFLICT).body(
         ErrorResponse.of("ORDER_NOT_ACKNOWLEDGED", ex.getMessage())
       );
     }
   }
   ```

4. Routes:
   - `POST /api/v1/orders/{orderId}/changes/approve`
   - `POST /api/v1/orders/{orderId}/changes/reject`

**Gherkin Scenarios**
Feature: Approve or Reject Order Changes REST Endpoints

Scenario: Approve order change (HTTP 200)
  Given an order "ord-001" with status "ACKNOWLEDGED"
  And a change request exists for the order
  And at least 1 prepared item can be transferred
  When a POST request is sent to `/api/v1/orders/ord-001/changes/approve`
  Then HTTP 200 OK response is returned
  And response message is "Change approved"
  And newEstimatedReadinessAt is provided
  And newEstimatedReadinessMinutes is provided

Scenario: Cannot approve - no transferable items (HTTP 409)
  Given an order "ord-002" with no prepared items
  And a change request exists
  When a POST request is sent to `/api/v1/orders/ord-002/changes/approve`
  Then HTTP 409 Conflict is returned
  And error code "CANNOT_APPROVE_CHANGE" is included

Scenario: Cannot approve if order not acknowledged (HTTP 409)
  Given an order "ord-003" with status "PENDING"
  When a POST request is sent to `/api/v1/orders/ord-003/changes/approve`
  Then HTTP 409 Conflict is returned
  And error code "ORDER_NOT_ACKNOWLEDGED" is included

Scenario: Reject order change (HTTP 200)
  Given an order "ord-004" with a change request
  When a POST request is sent to `/api/v1/orders/ord-004/changes/reject` with reason "Cannot prepare in time"
  Then HTTP 200 OK response is returned
  And response message is "Change rejected"
  And the provided reason is echoed back

Scenario: Cannot reject with empty reason (HTTP 400)
  Given a POST request with empty reason field
  When sent to `/api/v1/orders/ord-005/changes/reject`
  Then HTTP 400 Bad Request is returned
  And error code "INVALID_REASON" is included

Scenario: Cannot reject if order not acknowledged (HTTP 409)
  Given an order "ord-006" with status "PENDING"
  When a POST request is sent to `/api/v1/orders/ord-006/changes/reject`
  Then HTTP 409 Conflict is returned

Scenario: Order not found for approval (HTTP 404)
  Given a POST request to `/api/v1/orders/ord-999/changes/approve`
  When the order does not exist
  Then HTTP 404 Not Found is returned

Scenario: Order not found for rejection (HTTP 404)
  Given a POST request to `/api/v1/orders/ord-999/changes/reject`
  When the order does not exist
  Then HTTP 404 Not Found is returned

Scenario: Approve change with multiple prepared items of different types
  Given a POST request to `/api/v1/orders/ord-008/changes/approve`
  And the order contains 2 prepared beers and 1 prepared snack
  And the change request adds 1 snack
  When the bartender approves
  Then HTTP 200 OK is returned
  And response contains updated order with new item added
  And estimatedReadinessAt is recalculated

Scenario: Concurrent approve requests from different bartenders (race condition)
  Given an order with 1 pending change request
  And two bartenders submit approve requests simultaneously to `/api/v1/orders/ord-011/changes/approve`
  When both requests are processed concurrently
  Then one succeeds with HTTP 200
  And the other receives either HTTP 404 (change already processed) or HTTP 200 (idempotent)
  And order is updated exactly once

Scenario: Cannot approve if order is READY (already completed)
  Given an order "ord-012" with status "READY"
  And a change request exists for it
  When a POST request to `/api/v1/orders/ord-012/changes/approve` is made
  Then HTTP 409 Conflict is returned
  And error includes "Order is not in ACKNOWLEDGED state"

Scenario: Cannot approve if order is CANCELLED
  Given an order "ord-013" with status "CANCELLED"
  And a change request exists for it
  When a POST request to `/api/v1/orders/ord-013/changes/approve` is made
  Then HTTP 409 Conflict is returned
  And order status remains unchanged

Scenario: Bartender authentication required
  Given an unauthenticated request to `/api/v1/orders/ord-014/changes/approve`
  When no valid bartender token is provided
  Then HTTP 401 Unauthorized is returned
  And no changes are processed

**Notes**
- Approval endpoints are for bartenders only (staff).
- Both endpoints return HTTP 200 on success with different DTO structures.
- Rejection reason is required and validated.
- Order status remains ACKNOWLEDGED after both approval and rejection.
