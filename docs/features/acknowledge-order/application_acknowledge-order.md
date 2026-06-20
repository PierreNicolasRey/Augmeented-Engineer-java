# Application: Acknowledge an Order

**Context**
Bartenders need an HTTP endpoint to acknowledge orders and signal that preparation has begun. The endpoint must return the updated order details including the estimated time of readiness so the bartender can provide this information to the festival goer. The response should also reflect that tokens have been permanently deducted from the festival goer's balance.

**Problem**
The Application layer must provide a RESTful interface for acknowledging orders, mapping HTTP requests to domain commands and returning the updated order with estimated readiness time.

**Acceptance Criteria**
- [ ] HTTP endpoint created:
  - Method: `PUT /api/v1/orders/{orderId}/acknowledge`
  - Path parameter: `orderId` (string, required)
  - No request body (or minimal metadata like bartenderId for audit)
  
- [ ] `AcknowledgeOrderResponseDTO` structure:
  - Field: `orderId: String`
  - Field: `status: String` ("ACKNOWLEDGED")
  - Field: `estimatedReadinessAt: Instant` (when order will be ready)
  - Field: `estimatedReadinessMinutes: int` (for display, e.g., "13 minutes")
  - Field: `items: List<OrderItemResponseDTO>` (what's being prepared)
  - Field: `totalDrinkTokensDeducted: int` (breakdown)
  - Field: `totalSnackTokensDeducted: int` (breakdown)
  - Field: `acknowledgedAt: Instant` (when acknowledged)
  
- [ ] Controller method: `acknowledgeOrder(orderId): ResponseEntity<?>`
  - Validate orderId format (non-empty string)
  - Call `AcknowledgeOrderUseCase.execute(orderId)`
  - Fetch acknowledged order
  - Return 200 OK with `AcknowledgeOrderResponseDTO` on success
  - Handle exceptions (see error responses below)
  
- [ ] Success response (HTTP 200):
  - Body: `AcknowledgeOrderResponseDTO` with order details, estimated readiness, and token deductions
  - Include items being prepared and their preparation times
  
- [ ] Error response HTTP 400 (Bad Request):
  - Scenario: Invalid orderId format (empty, null)
  - Response: `{ "error": "INVALID_ORDER_ID", "message": "orderId must be a non-empty string" }`
  
- [ ] Error response HTTP 404 (Not Found):
  - Scenario: Order not found
  - Response: `{ "error": "ORDER_NOT_FOUND", "message": "Order {orderId} not found" }`
  
- [ ] `OrderQueryService` injected in controller:
  - Used to fetch acknowledged order after use case execution
  - Method: `findOrderById(orderId): Order` (CQS pattern, not repository)

- [ ] Error response HTTP 409 (Conflict):
  - Scenario: Order cannot be acknowledged (not in PENDING state)
  - Response: `{ "error": "ORDER_CANNOT_BE_ACKNOWLEDGED", "message": "Order {orderId} has status {status} and cannot be acknowledged. Only PENDING orders can be acknowledged" }`
  
- [ ] Error response HTTP 404 (Not Found):
  - Scenario: Festival goer not found
  - Response: `{ "error": "FESTIVAL_GOER_NOT_FOUND", "message": "Festival goer not found" }`
  
- [ ] Request/Response DTOs created:
  - `AcknowledgeOrderResponseDTO` in application/dto/response
  - Reuse `OrderItemResponseDTO` from place-order feature
  
- [ ] Mapper created:
  - `AcknowledgeOrderResponseMapper` in application/mapper
  - Method: `toDTO(Order): AcknowledgeOrderResponseDTO`
  
- [ ] Controller method routing:
  - Endpoint path: `PUT /api/v1/orders/{orderId}/acknowledge`
  - Consumes: application/json (or no body)
  - Produces: application/json
  - Status codes documented: 200, 400, 404, 409

**Implementation Plan**
1. Create DTOs in application/dto/response:
   
   - `AcknowledgeOrderResponseDTO`:
     ```java
     public record AcknowledgeOrderResponseDTO(
       String orderId,
       String status,
       Instant estimatedReadinessAt,
       int estimatedReadinessMinutes,
       List<OrderItemResponseDTO> items,
       int totalDrinkTokensDeducted,
       int totalSnackTokensDeducted,
       Instant acknowledgedAt
     ) {}
     ```

2. Create mapper in application/mapper:
   
   - `AcknowledgeOrderResponseMapper`:
     ```java
     public class AcknowledgeOrderResponseMapper {
       public AcknowledgeOrderResponseDTO toDTO(Order order) {
         return new AcknowledgeOrderResponseDTO(
           order.getId(),
           "ACKNOWLEDGED",
           order.getEstimatedReadinessAt(),
           order.getEstimatedReadinessMinutes(), // Already calculated in domain
           mapItems(order.getItems()),
           order.getTotalDrinkTokensDeducted(),
           order.getTotalSnackTokensDeducted(),
           order.getUpdatedAt()
         );
       }
       
       private List<OrderItemResponseDTO> mapItems(List<OrderItem> items) { ... }
     }
     ```

3. Create controller method in `BartenderController` (new) or `OrderController`:
   
   ```java
   @PutMapping("/{orderId}/acknowledge")
   public ResponseEntity<?> acknowledgeOrder(
     @PathVariable String orderId
   ) {
     try {
       // Validate orderId
       if (orderId == null || orderId.isBlank()) {
         return ResponseEntity.badRequest().body(
           ErrorResponse.of("INVALID_ORDER_ID", "orderId must be a non-empty string")
         );
       }
       
       // Call use case
       acknowledgeOrderUseCase.execute(orderId);
       
       // Fetch acknowledged order via query service (CQS pattern, not repository)
       Order acknowledgedOrder = orderQueryService.findOrderById(orderId)
         .orElseThrow(() -> new OrderNotFoundException(orderId));
       
       // Map and return response
       AcknowledgeOrderResponseDTO response = acknowledgeOrderResponseMapper.toDTO(acknowledgedOrder);
       return ResponseEntity.ok(response);
       
     } catch (OrderNotFoundException ex) {
       return ResponseEntity.notFound().build();
     } catch (OrderCannotBeAcknowledgedException ex) {
       return ResponseEntity.status(HttpStatus.CONFLICT).body(
         ErrorResponse.of("ORDER_CANNOT_BE_ACKNOWLEDGED", ex.getMessage())
       );
     } catch (FestivalGoerNotFoundException ex) {
       return ResponseEntity.notFound().body(
         ErrorResponse.of("FESTIVAL_GOER_NOT_FOUND", ex.getMessage())
       );
     }
   }
   ```

4. Inject dependencies in controller:
   - `AcknowledgeOrderUseCase acknowledgeOrderUseCase`
   - `OrderQueryService orderQueryService` (for CQS-compliant read after write)
   - `AcknowledgeOrderResponseMapper acknowledgeOrderResponseMapper`

5. Add route to controller:
   - Method: `acknowledgeOrder(orderId)`
   - Path: `PUT /api/v1/orders/{orderId}/acknowledge`

6. Response mapping:
   - Extract order details from domain entity (estimatedReadinessMinutes already calculated)
   - Map items, tokens deducted, and estimated readiness
   - **NOTE**: Do NOT calculate time in mapper; use `Order.getEstimatedReadinessMinutes()` computed in domain

7. Error handling:
   - Map domain exceptions to HTTP status codes
   - Include order status in conflict response

**Gherkin Scenarios**
Feature: Acknowledge an Order REST Endpoint

Scenario: Acknowledge pending order (HTTP 200)
  Given a bartender has access to the endpoint
  And an order "ord-001" exists in PENDING status
  And the order contains 1 normal alcoholic drink
  When a PUT request is sent to `/api/v1/orders/ord-001/acknowledge`
  Then HTTP 200 OK response is returned
  And response body contains orderId "ord-001"
  And response status is "ACKNOWLEDGED"
  And estimatedReadinessMinutes is 2 (1 item × 2 minutes per normal drink)
  And totalDrinkTokensDeducted shows correct amount
  And acknowledgedAt timestamp is set

Scenario: Acknowledge order with multiple drink items
  Given an order "ord-002" containing:
    - 5 non-alcoholic drinks (same type)
    - 1 normal alcoholic drink
    - 1 premium alcoholic drink
  When a PUT request is sent to `/api/v1/orders/ord-002/acknowledge`
  Then HTTP 200 OK response is returned
  And estimatedReadinessMinutes is 10 (5×1 + 1×2 + 1×3)
  And response includes all 7 items in the items list

Scenario: Acknowledge order with mixed items (meals + drinks)
  Given an order "ord-003" containing 1 meal and 1 premium drink
  When the order is acknowledged
  Then estimatedReadinessMinutes is 13 (10 for meal + 3 for premium drink in parallel)
  And response includes meal and drink items

Scenario: Acknowledge order with only non-alcoholic items
  Given an order "ord-004" with 0 drink tokens reserved
  When the order is acknowledged
  Then totalDrinkTokensDeducted shows 0
  And totalSnackTokensDeducted shows appropriate amount

Scenario: Cannot acknowledge already acknowledged order (HTTP 409)
  Given an order "ord-005" with status "ACKNOWLEDGED"
  When a PUT request is sent to `/api/v1/orders/ord-005/acknowledge`
  Then HTTP 409 Conflict is returned
  And error code "ORDER_CANNOT_BE_ACKNOWLEDGED" is included

Scenario: Cannot acknowledge ready order (HTTP 409)
  Given an order "ord-006" with status "READY"
  When a PUT request is sent to `/api/v1/orders/ord-006/acknowledge`
  Then HTTP 409 Conflict is returned

Scenario: Cannot acknowledge cancelled order (HTTP 409)
  Given an order "ord-007" with status "CANCELLED"
  When a PUT request is sent to `/api/v1/orders/ord-007/acknowledge`
  Then HTTP 409 Conflict is returned

Scenario: Order not found (HTTP 404)
  Given a PUT request is sent to `/api/v1/orders/ord-999/acknowledge`
  When the order does not exist
  Then HTTP 404 Not Found is returned
  And error code "ORDER_NOT_FOUND" is included

Scenario: Festival goer not found (HTTP 404)
  Given an order "ord-008" references non-existent festival goer
  When a PUT request is sent to `/api/v1/orders/ord-008/acknowledge`
  Then HTTP 404 Not Found is returned
  And error code "FESTIVAL_GOER_NOT_FOUND" is included

Scenario: Invalid orderId format (HTTP 400)
  Given a PUT request with empty orderId
  When sent to `/api/v1/orders//acknowledge`
  Then HTTP 404 Not Found (route not matched) or HTTP 400 Bad Request

Scenario: Response includes items breakdown
  Given an order "ord-009" with 3 items
  When the order is acknowledged
  Then the response includes items list with all 3 items
  And each item includes type, quantity, and preparation time contribution

Scenario: Estimated readiness time is in future
  Given an order is acknowledged at time T1
  When the response is returned with estimatedReadinessAt
  Then estimatedReadinessAt is > T1
  And estimatedReadinessMinutes is > 0

**Notes**
- PUT semantics: Idempotent in intent (acknowledging an already-acknowledged order fails with HTTP 409).
- No request body required; the path parameter uniquely identifies the order.
- Response includes estimated readiness in both `Instant` (for internal use) and minutes (for display).
- Token deductions are reflected in the response to show tokens were consumed.
- The endpoint is typically for bartenders/staff, not festival goers.
- Domain events are published internally; the response is just the HTTP confirmation.
- **CQS Pattern**: After executing the write (use case), read the acknowledged order via `OrderQueryService`, not the persistence repository. The query service is responsible for efficient, read-optimized access.
- **No Business Logic in Mapper**: The mapper is a pure DTO transformer. Time calculations must be done in the domain (Order entity stores `estimatedReadinessMinutes`), not in the application layer.
