# Application: Cancel an Order

**Context**
Festival goers need an HTTP endpoint to cancel their pending orders and receive confirmation that the order has been cancelled and tokens have been refunded. The endpoint must validate the order exists, prevent cancellation of non-pending orders, and return appropriate responses.

**Problem**
The Application layer must provide a RESTful interface for cancelling orders, mapping HTTP requests to domain commands and returning properly formatted responses with confirmation details.

**Acceptance Criteria**
- [ ] HTTP endpoint created:
  - Method: `DELETE /api/v1/orders/{orderId}`
  - Path parameter: `orderId` (string, required)
  - No request body
  
- [ ] `CancelOrderResponseDTO` structure:
  - Field: `orderId: String`
  - Field: `status: String` ("CANCELLED")
  - Field: `message: String` (e.g., "Order cancelled successfully. Tokens have been refunded.")
  - Field: `refundedTokens: TokenRefundDTO` (breakdown of tokens refunded)
  - Field: `availableTokensAfter: AvailableTokensDTO` (updated available balance)
  - Field: `cancelledAt: Instant` (timestamp)
  
- [ ] `TokenRefundDTO` structure:
  - Field: `drinkTokens: int` (tokens refunded)
  - Field: `snackTokens: int` (tokens refunded)
  
- [ ] `AvailableTokensDTO` structure:
  - Field: `drinkTokens: int` (available after refund)
  - Field: `snackTokens: int` (available after refund)
  
- [ ] Controller method: `cancelOrder(orderId): ResponseEntity<?>`
  - Validate orderId format (non-empty string)
  - Call `CancelOrderUseCase.execute(orderId)`
  - Fetch cancelled order and current token balance
  - Return 200 OK with `CancelOrderResponseDTO` on success
  - Handle exceptions (see error responses below)
  
- [ ] Success response (HTTP 200):
  - Body: `CancelOrderResponseDTO` with cancelled order details and refund confirmation
  
- [ ] Error response HTTP 400 (Bad Request):
  - Scenario: Invalid orderId format (empty, null, etc.)
  - Response: `{ "error": "INVALID_ORDER_ID", "message": "orderId must be a non-empty string" }`
  
- [ ] Error response HTTP 404 (Not Found):
  - Scenario: Order not found
  - Response: `{ "error": "ORDER_NOT_FOUND", "message": "Order {orderId} not found" }`
  
- [ ] Error response HTTP 409 (Conflict):
  - Scenario: Order cannot be cancelled (already acknowledged/ready/cancelled)
  - Response: `{ "error": "ORDER_CANNOT_BE_CANCELLED", "message": "Order {orderId} has status {status} and cannot be cancelled. Only PENDING orders can be cancelled" }`
  
- [ ] Error response HTTP 404 (Not Found):
  - Scenario: Festival goer not found
  - Response: `{ "error": "FESTIVAL_GOER_NOT_FOUND", "message": "Festival goer not found" }`
  
- [ ] Request/Response DTOs created:
  - `CancelOrderResponseDTO` in application/dto/response
  - `TokenRefundDTO` in application/dto/response
  - `AvailableTokensDTO` in application/dto/response (reuse if exists from consult-token-balance)
  
- [ ] Mappers created:
  - `CancelOrderResponseMapper` in application/mapper
  - Method: `toDTO(Order, TokenBalance): CancelOrderResponseDTO` (pure transformer, no logic)
  
- [ ] Controller method routing:
  - Endpoint path: `DELETE /api/v1/orders/{orderId}`
  - Consumes: no body (DELETE semantics)
  - Produces: application/json
  - Status codes documented: 200, 400, 404, 409

**Implementation Plan**
1. Create DTOs in application/dto/response:
   
   - `TokenRefundDTO`:
     ```java
     public record TokenRefundDTO(
       int drinkTokens,
       int snackTokens
     ) {}
     ```
   
   - `AvailableTokensDTO`:
     ```java
     public record AvailableTokensDTO(
       int drinkTokens,
       int snackTokens
     ) {}
     ```
   
   - `CancelOrderResponseDTO`:
     ```java
     public record CancelOrderResponseDTO(
       String orderId,
       String status,
       String message,
       TokenRefundDTO refundedTokens,
       AvailableTokensDTO availableTokensAfter,
       Instant cancelledAt
     ) {}
     ```

2. Create mapper in application/mapper:
   
   - `CancelOrderResponseMapper`:
     ```java
     public class CancelOrderResponseMapper {
       public CancelOrderResponseDTO toDTO(Order order, TokenBalance tokenBalance) {
         TokenRefundDTO refund = new TokenRefundDTO(
           order.getReservedDrinkTokens(),
           order.getReservedSnackTokens()
         );
         AvailableTokensDTO available = new AvailableTokensDTO(
           tokenBalance.getAvailableDrinkTokens(),
           tokenBalance.getAvailableSnackTokens()
         );
         return new CancelOrderResponseDTO(
           order.getId(),
           "CANCELLED",
           "Order cancelled successfully. Tokens have been refunded.",
           refund,
           available,
           order.getUpdatedAt()
         );
       }
     }
     ```

3. Create controller method in `OrderController`:
   
   ```java
   @DeleteMapping("/{orderId}")
   public ResponseEntity<?> cancelOrder(
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
       cancelOrderUseCase.execute(orderId);
       
       // Fetch cancelled order via query service (CQS pattern, not repository)
       Order cancelledOrder = orderQueryService.findOrderById(orderId)
         .orElseThrow(() -> new OrderNotFoundException(orderId));
       
       // Fetch token balance via query service (CQS pattern)
       TokenBalance balance = tokenBalanceQueryService.findTokenBalanceByFestivalGoerId(
         cancelledOrder.getFestivalGoerId()
       ).orElseThrow();
       
       // Map and return response
       CancelOrderResponseDTO response = cancelOrderResponseMapper.toDTO(cancelledOrder, balance);
       return ResponseEntity.ok(response);
       
     } catch (OrderNotFoundException ex) {
       return ResponseEntity.notFound().build();
     } catch (OrderCannotBeCancelledException ex) {
       return ResponseEntity.status(HttpStatus.CONFLICT).body(
         ErrorResponse.of("ORDER_CANNOT_BE_CANCELLED", ex.getMessage())
       );
     } catch (FestivalGoerNotFoundException ex) {
       return ResponseEntity.notFound().body(
         ErrorResponse.of("FESTIVAL_GOER_NOT_FOUND", ex.getMessage())
       );
     }
   }
   ```

4. Add route to `OrderController`:
   - Method: `cancelOrder(orderId)`
   - Path: `DELETE /api/v1/orders/{orderId}`

5. Response mapping:
   - Create mapper: `CancelOrderResponseMapper`
   - Extract refunded tokens, available tokens, timestamp

6. Error handling:
   - Map domain exceptions to HTTP status codes
   - Include order status in conflict response

**Gherkin Scenarios**
Feature: Cancel an Order REST Endpoint

Scenario: Cancel pending order (HTTP 200)
  Given a festival goer is authenticated
  And an order "ord-001" exists in PENDING status
  And the order reserved 1 drink token, 0 snack tokens
  And the festival goer has 5 available drink tokens, 9 available snack tokens
  When a DELETE request is sent to `/api/v1/orders/ord-001`
  Then HTTP 200 OK response is returned
  And response body contains orderId "ord-001"
  And response status is "CANCELLED"
  And refundedTokens shows drink: 1, snack: 0
  And availableTokensAfter shows drink: 6 (5+1), snack: 9 (9+0)
  And message confirms cancellation and refund

Scenario: Confirm tokens are refunded immediately
  Given a festival goer with 2 available drink tokens before cancellation
  And an order reserved 2 drink tokens
  When the order is cancelled via DELETE
  Then the response shows availableTokensAfter: drink: 4 (2+2)

Scenario: Cannot cancel acknowledged order (HTTP 409)
  Given an order "ord-002" with status "ACKNOWLEDGED"
  When a DELETE request is sent to `/api/v1/orders/ord-002`
  Then HTTP 409 Conflict is returned
  And error code "ORDER_CANNOT_BE_CANCELLED" is included
  And message indicates status is ACKNOWLEDGED

Scenario: Cannot cancel ready order (HTTP 409)
  Given an order "ord-003" with status "READY"
  When a DELETE request is sent to `/api/v1/orders/ord-003`
  Then HTTP 409 Conflict is returned

Scenario: Cannot cancel already cancelled order (HTTP 409)
  Given an order "ord-004" with status "CANCELLED"
  When a DELETE request is sent to `/api/v1/orders/ord-004`
  Then HTTP 409 Conflict is returned

Scenario: Order not found (HTTP 404)
  Given a DELETE request is sent to `/api/v1/orders/ord-999`
  When the order does not exist
  Then HTTP 404 Not Found is returned
  And error code "ORDER_NOT_FOUND" is included

Scenario: Festival goer not found (HTTP 404)
  Given an order "ord-005" references non-existent festival goer
  When a DELETE request is sent to `/api/v1/orders/ord-005`
  Then HTTP 404 Not Found is returned
  And error code "FESTIVAL_GOER_NOT_FOUND" is included

Scenario: Invalid orderId format (HTTP 400)
  Given a DELETE request with empty orderId
  When sent to `/api/v1/orders/`
  Then HTTP 404 Not Found is returned (route not matched)

Scenario: Null orderId (HTTP 400)
  Given a DELETE request with orderId = null
  When sent to the endpoint
  Then HTTP 400 Bad Request is returned
  And error code "INVALID_ORDER_ID" is included

Scenario: Cancel order with mixed tokens reserved
  Given an order "ord-006" reserved 2 drink tokens, 3 snack tokens
  And the festival goer has 3 available drink, 6 available snack before cancel
  When the order is cancelled
  Then the response shows:
    - refundedTokens: drink: 2, snack: 3
    - availableTokensAfter: drink: 5 (3+2), snack: 9 (6+3)

Scenario: Cancel order with no tokens reserved
  Given an order "ord-007" reserved 0 drink, 0 snack (edge case, non-alcoholic items only, no food)
  When the order is cancelled
  Then the response shows refundedTokens: drink: 0, snack: 0
  And availableTokensAfter unchanged

**Notes**
- DELETE semantics: No request body is expected.
- Success response includes refund breakdown so the festival goer understands exactly what was refunded.
- HTTP 409 is used for order state conflicts (cannot cancel non-pending orders).
- HTTP 404 is used for missing orders or festival goers.
- HTTP 400 is used for invalid input parameters.
- All HTTP responses include proper content-type: application/json.
- Cancelled orders cannot be reactivated or modified; a new order must be placed instead.
