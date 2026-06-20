# Application: Mark an Order as Ready

**Context**
Bartenders need an HTTP endpoint to mark orders as ready for pickup. The endpoint must confirm the order status change and return the updated order details.

**Problem**
The Application layer must provide a RESTful interface for marking orders as ready, mapping HTTP requests to domain commands and returning confirmation.

**Acceptance Criteria**
- [ ] HTTP endpoint created:
  - Method: `PUT /api/v1/orders/{orderId}/ready`
  - Path parameter: `orderId` (string, required)
  - No request body
  
- [ ] `MarkReadyResponseDTO` structure:
  - Field: `orderId: String`
  - Field: `status: String` ("READY")
  - Field: `readyAt: Instant`
  - Field: `message: String` (e.g., "Order is ready for pickup")
  
- [ ] Controller method: `markOrderReady(orderId): ResponseEntity<?>`
  - Validate orderId format
  - Call `MarkOrderReadyUseCase.execute(orderId)`
  - Fetch updated order
  - Return 200 OK with response on success
  - Handle exceptions
  
- [ ] Success response (HTTP 200):
  - Body: `MarkReadyResponseDTO` with confirmation
  
- [ ] Error response HTTP 404 (Not Found):
  - Scenario: Order not found
  - Response: `{ "error": "ORDER_NOT_FOUND", "message": "Order {orderId} not found" }`
  
- [ ] Error response HTTP 409 (Conflict):
  - Scenario: Order cannot be marked ready (not in ACKNOWLEDGED state)
  - Response: `{ "error": "ORDER_CANNOT_BE_MARKED_READY", "message": "Order {orderId} has status {status} and cannot be marked ready. Only ACKNOWLEDGED orders can be marked as ready" }`
  
- [ ] Request/Response DTOs created:
  - `MarkReadyResponseDTO` in application/dto/response
  
- [ ] Mapper created:
  - `MarkReadyResponseMapper` in application/mapper
  
- [ ] Controller method routing:
  - Endpoint path: `PUT /api/v1/orders/{orderId}/ready`
  - Produces: application/json
  - Status codes: 200, 404, 409

**Implementation Plan**
1. Create DTO in application/dto/response:
   
   - `MarkReadyResponseDTO`:
     ```java
     public record MarkReadyResponseDTO(
       String orderId,
       String status,
       Instant readyAt,
       String message
     ) {}
     ```

2. Create mapper in application/mapper:
   
   - `MarkReadyResponseMapper`:
     ```java
     public MarkReadyResponseDTO toDTO(Order order) {
       return new MarkReadyResponseDTO(
         order.getId(),
         "READY",
         order.getReadyAt(),
         "Order is ready for pickup"
       );
     }
     ```

3. Add controller method:
   
   ```java
   @PutMapping("/{orderId}/ready")
   public ResponseEntity<?> markOrderReady(
     @PathVariable String orderId
   ) {
     try {
       markOrderReadyUseCase.execute(orderId);
       // Fetch order via query service (CQS pattern, not repository)
       Order readyOrder = orderQueryService.findOrderById(orderId)
         .orElseThrow(() -> new OrderNotFoundException(orderId));
       return ResponseEntity.ok(markReadyResponseMapper.toDTO(readyOrder));
     } catch (OrderNotFoundException ex) {
       return ResponseEntity.notFound().build();
     } catch (OrderCannotBeMarkedReadyException ex) {
       return ResponseEntity.status(HttpStatus.CONFLICT).body(
         ErrorResponse.of("ORDER_CANNOT_BE_MARKED_READY", ex.getMessage())
       );
     }
   }
   ```

4. Route: `PUT /api/v1/orders/{orderId}/ready`

**Gherkin Scenarios**
Feature: Mark an Order as Ready REST Endpoint

Scenario: Mark acknowledged order as ready (HTTP 200)
  Given an order "ord-001" with status "ACKNOWLEDGED"
  When a PUT request is sent to `/api/v1/orders/ord-001/ready`
  Then HTTP 200 OK response is returned
  And response status is "READY"
  And readyAt timestamp is set
  And message confirms pickup readiness

Scenario: Cannot mark pending order as ready (HTTP 409)
  Given an order "ord-002" with status "PENDING"
  When a PUT request is sent to `/api/v1/orders/ord-002/ready`
  Then HTTP 409 Conflict is returned
  And error code "ORDER_CANNOT_BE_MARKED_READY" is included

Scenario: Cannot mark already ready order (HTTP 409)
  Given an order "ord-003" with status "READY"
  When a PUT request is sent to `/api/v1/orders/ord-003/ready`
  Then HTTP 409 Conflict is returned

Scenario: Cannot mark cancelled order as ready (HTTP 409)
  Given an order "ord-004" with status "CANCELLED"
  When a PUT request is sent to `/api/v1/orders/ord-004/ready`
  Then HTTP 409 Conflict is returned

Scenario: Order not found (HTTP 404)
  Given a PUT request to `/api/v1/orders/ord-999/ready`
  When the order does not exist
  Then HTTP 404 Not Found is returned

**Notes**
- Simple endpoint for state transition confirmation.
- No complex business logic; primarily a status update.
