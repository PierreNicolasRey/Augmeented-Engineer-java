# Application: Change an Order

**Context**
Festival goers need an HTTP endpoint to modify their pending orders. The endpoint must accept requests to add and remove items, validate the request format, and return appropriate responses indicating success or the specific reason for failure (e.g., insufficient tokens, order acknowledged, invalid items).

**Problem**
The Application layer must provide a RESTful interface for changing orders, mapping HTTP requests to domain commands and returning properly formatted responses with detailed error information.

**Acceptance Criteria**
- [ ] HTTP endpoint created:
  - Method: `PATCH /api/v1/orders/{orderId}`
  - Path parameter: `orderId` (string, required)
  - Request body: `ChangeOrderRequestDTO`
  
- [ ] `ChangeOrderRequestDTO` structure:
  - Field: `itemsToAdd: List<OrderItemRequestDTO>` (optional, default empty list)
  - Field: `itemsToRemove: List<String>` (list of item IDs, optional, default empty list)
  - Validation: at least one of add/remove must be provided (non-empty)
  - Validation: each item in itemsToAdd must be valid (quantity > 0, type specified)
  - Validation: itemsToRemove must contain valid item IDs
  
- [ ] `OrderItemRequestDTO` structure (reuse from place-order):
  - Field: `itemType: String` (DRINK or FOOD, required)
  - Field: `itemId: String` (menu item ID, required)
  - Field: `quantity: int` (positive integer, required)
  - Validation: quantity >= 1
  
- [ ] `ChangeOrderResponseDTO` structure:
  - Field: `orderId: String`
  - Field: `status: String` (PENDING)
  - Field: `totalDrinkTokensCost: int`
  - Field: `totalSnackTokensCost: int`
  - Field: `items: List<OrderItemResponseDTO>`
  - Field: `updatedAt: Instant`
  
- [ ] Controller method: `changeOrder(orderId, changeOrderRequestDTO): ResponseEntity<?>`
  - Validate orderId format (non-empty string)
  - Validate request body not null
  - Call `ChangeOrderUseCase.execute(orderId, itemsToAdd, itemsToRemove)`
  - Return 200 OK with `ChangeOrderResponseDTO` on success
  - Handle exceptions (see error responses below)
  
- [ ] Success response (HTTP 200):
  - Body: `ChangeOrderResponseDTO` with updated order details
  - Include updated timestamp
  
- [ ] Error response HTTP 400 (Bad Request):
  - Scenario: Empty request (no items to add, no items to remove)
  - Response: `{ "error": "REQUEST_INVALID", "message": "At least one of itemsToAdd or itemsToRemove must be provided" }`
  
- [ ] Error response HTTP 400 (Bad Request):
  - Scenario: Invalid item quantity
  - Response: `{ "error": "INVALID_ITEM_QUANTITY", "message": "Item quantity must be greater than 0" }`
  
- [ ] Error response HTTP 404 (Not Found):
  - Scenario: Order not found
  - Response: `{ "error": "ORDER_NOT_FOUND", "message": "Order {orderId} not found" }`
  
- [ ] Error response HTTP 409 (Conflict):
  - Scenario: Order cannot be changed (already acknowledged/ready/cancelled)
  - Response: `{ "error": "ORDER_CANNOT_BE_CHANGED", "message": "Order {orderId} has status {status} and cannot be modified. Only PENDING orders can be changed" }`
  
- [ ] Error response HTTP 422 (Unprocessable Entity):
  - Scenario: Insufficient tokens for the new order total
  - Response body with token breakdown:
    ```json
    {
      "error": "INSUFFICIENT_TOKENS",
      "message": "Insufficient tokens to complete the order",
      "details": {
        "required": {
          "drinkTokens": 2,
          "snackTokens": 5
        },
        "available": {
          "drinkTokens": 1,
          "snackTokens": 3
        },
        "shortage": {
          "drinkTokens": 1,
          "snackTokens": 2
        }
      }
    }
    ```
  
- [ ] Error response HTTP 404 (Not Found):
  - Scenario: Festival goer not found
  - Response: `{ "error": "FESTIVAL_GOER_NOT_FOUND", "message": "Festival goer not found" }`
  
- [ ] Request/Response DTOs created:
  - `ChangeOrderRequestDTO` in application/dto/request
  - `ChangeOrderResponseDTO` in application/dto/response
  - `OrderItemRequestDTO` reused from place-order
  - `OrderItemResponseDTO` reused from place-order
  
- [ ] `OrderQueryService` injected in controller:
  - Used to fetch updated order after use case execution
  - Method: `findOrderById(orderId): Optional<Order>` (CQS pattern, not repository)

- [ ] Mapper created:
  - `ChangeOrderRequestMapper` in application/mapper
  - Method: `toUseCase(ChangeOrderRequestDTO): (List<OrderItem>, List<String>)` for add/remove items
  - Reuse existing mappers for OrderItem conversion
  
- [ ] Controller method routing:
  - Endpoint path: `PATCH /api/v1/orders/{orderId}`
  - Consumes: application/json
  - Produces: application/json
  - Status codes documented: 200, 400, 404, 409, 422

**Implementation Plan**
1. Create DTOs in application/dto/request and response:
   
   - `ChangeOrderRequestDTO`:
     ```java
     public record ChangeOrderRequestDTO(
       @NotNull(message = "itemsToAdd cannot be null")
       @NotEmpty(message = "At least one of itemsToAdd or itemsToRemove must be provided")
       List<OrderItemRequestDTO> itemsToAdd,
       
       @NotNull(message = "itemsToRemove cannot be null")
       List<String> itemsToRemove
     ) {}
     ```
     - Validation: Custom validator to ensure at least one list is non-empty
   
   - `ChangeOrderResponseDTO`:
     ```java
     public record ChangeOrderResponseDTO(
       String orderId,
       String status,
       int totalDrinkTokensCost,
       int totalSnackTokensCost,
       List<OrderItemResponseDTO> items,
       Instant updatedAt
     ) {}
     ```

2. Create mapper in application/mapper:
   
   - `ChangeOrderRequestMapper`:
     ```java
     public class ChangeOrderRequestMapper {
       public ChangeOrderCommand toCommand(String orderId, ChangeOrderRequestDTO dto) {
         List<OrderItem> itemsToAdd = dto.itemsToAdd().stream()
           .map(this::toOrderItem)
           .toList();
         List<String> itemsToRemove = dto.itemsToRemove() != null 
           ? dto.itemsToRemove() 
           : List.of();
         return new ChangeOrderCommand(orderId, itemsToAdd, itemsToRemove);
       }
       
       private OrderItem toOrderItem(OrderItemRequestDTO dto) { ... }
     }
     ```

3. Create controller method in `OrderController`:
   
   ```java
   @PatchMapping("/{orderId}")
   public ResponseEntity<?> changeOrder(
     @PathVariable String orderId,
     @Valid @RequestBody ChangeOrderRequestDTO request
   ) {
     try {
       // Validate request
       if ((request.itemsToAdd() == null || request.itemsToAdd().isEmpty())
           && (request.itemsToRemove() == null || request.itemsToRemove().isEmpty())) {
         return ResponseEntity.badRequest().body(
           ErrorResponse.of("REQUEST_INVALID", "At least one of itemsToAdd or itemsToRemove must be provided")
         );
       }
       
       // Call use case
       changeOrderUseCase.execute(orderId, request.itemsToAdd(), request.itemsToRemove());
       
       // Fetch updated order via query service (CQS pattern, not repository)
       Order updatedOrder = orderQueryService.findOrderById(orderId)
         .orElseThrow(() -> new OrderNotFoundException(orderId));
       ChangeOrderResponseDTO response = changeOrderResponseMapper.toDTO(updatedOrder);
       return ResponseEntity.ok(response);
       
     } catch (OrderNotFoundException ex) {
       return ResponseEntity.notFound().build();
     } catch (OrderCannotBeChangedException ex) {
       return ResponseEntity.status(HttpStatus.CONFLICT).body(
         ErrorResponse.of("ORDER_CANNOT_BE_CHANGED", ex.getMessage())
       );
     } catch (InsufficientTokensException ex) {
       return ResponseEntity.unprocessableEntity().body(ex.toErrorResponse());
     } catch (FestivalGoerNotFoundException ex) {
       return ResponseEntity.notFound().body(
         ErrorResponse.of("FESTIVAL_GOER_NOT_FOUND", ex.getMessage())
       );
     }
   }
   ```

4. Add route to `OrderController`:
   - Method: `changeOrder(orderId, changeOrderRequestDTO)`
   - Path: `PATCH /api/v1/orders/{orderId}`

5. Validation in ChangeOrderRequestDTO:
   - At least one of itemsToAdd or itemsToRemove must be non-empty
   - Each OrderItemRequestDTO must have quantity > 0
   - Implement custom `@interface ValidChangeOrderRequest` validator

6. Response mapping:
   - Create mapper: `ChangeOrderResponseMapper.toDTO(Order): ChangeOrderResponseDTO`
   - Extract items, costs, timestamp from Order entity

7. Error handling:
   - Map domain exceptions to HTTP status codes
   - Include detailed token shortage breakdown in 422 response

**Gherkin Scenarios**
Feature: Change an Order REST Endpoint

Scenario: Change pending order with add items (HTTP 200)
  Given a festival goer is authenticated with ID "fgv-001"
  And an order "ord-001" exists in PENDING status with 1 item (cost: 1 drink)
  When a PATCH request is sent to `/api/v1/orders/ord-001` with itemsToAdd=[{snack, id, 1}]
  Then HTTP 200 response is returned
  And response body contains the updated order details
  And the new total cost reflects the added item
  And "updatedAt" timestamp is changed

Scenario: Change pending order with remove items (HTTP 200)
  Given a festival goer is authenticated
  And an order "ord-002" exists in PENDING status with 2 items
  When a PATCH request is sent with itemsToRemove=[first_item_id]
  Then HTTP 200 response is returned
  And the order now contains 1 item
  And the total cost is recalculated

Scenario: Change order with both add and remove (HTTP 200)
  Given a festival goer with an order "ord-003" in PENDING status
  When a PATCH request adds 1 item and removes 1 item (replace scenario)
  Then HTTP 200 response is returned
  And the order reflects both changes
  And token costs are recalculated correctly

Scenario: Empty change request (HTTP 400)
  Given a PATCH request is sent to `/api/v1/orders/ord-004`
  When itemsToAdd=[] and itemsToRemove=[]
  Then HTTP 400 Bad Request is returned
  And error code "REQUEST_INVALID" is included
  And message "At least one of itemsToAdd or itemsToRemove must be provided"

Scenario: Invalid item quantity (HTTP 400)
  Given a PATCH request with itemsToAdd=[{snack, id, 0}] (quantity zero)
  When sent to `/api/v1/orders/ord-005`
  Then HTTP 400 Bad Request is returned
  And error code "INVALID_ITEM_QUANTITY" is included

Scenario: Order not found (HTTP 404)
  Given a PATCH request is sent to `/api/v1/orders/ord-999`
  When the order does not exist
  Then HTTP 404 Not Found is returned
  And error code "ORDER_NOT_FOUND" is included

Scenario: Cannot change acknowledged order (HTTP 409)
  Given an order "ord-006" with status "ACKNOWLEDGED"
  When a PATCH request is sent to change the order
  Then HTTP 409 Conflict is returned
  And error code "ORDER_CANNOT_BE_CHANGED" is included
  And message includes current status

Scenario: Cannot change ready order (HTTP 409)
  Given an order "ord-007" with status "READY"
  When a PATCH request is sent to change the order
  Then HTTP 409 Conflict is returned

Scenario: Cannot change cancelled order (HTTP 409)
  Given an order "ord-008" with status "CANCELLED"
  When a PATCH request is sent to change the order
  Then HTTP 409 Conflict is returned

Scenario: Insufficient tokens after change (HTTP 422)
  Given a festival goer with 2 drink tokens, 5 snack tokens available
  And an order "ord-009" in PENDING status
  When a PATCH request adds items that would cost 3 drink tokens, 8 snack tokens total
  Then HTTP 422 Unprocessable Entity is returned
  And error code "INSUFFICIENT_TOKENS" is included
  And response includes:
    - required: {drinkTokens: 3, snackTokens: 8}
    - available: {drinkTokens: 2, snackTokens: 5}
    - shortage: {drinkTokens: 1, snackTokens: 3}

Scenario: Festival goer not found (HTTP 404)
  Given an order "ord-010" references non-existent festival goer
  When a PATCH request is sent to change the order
  Then HTTP 404 Not Found is returned
  And error code "FESTIVAL_GOER_NOT_FOUND" is included

Scenario: Invalid JSON request format (HTTP 400)
  Given a PATCH request with malformed JSON body
  When sent to `/api/v1/orders/ord-011`
  Then HTTP 400 Bad Request is returned
  And validation error is included

Scenario: Missing orderId path parameter (HTTP 404)
  Given a PATCH request to `/api/v1/orders/`
  When no orderId is provided
  Then HTTP 404 Not Found is returned (route not matched)

**Notes**
- This endpoint allows modification of PENDING orders only. Acknowledged orders return HTTP 409.
- The request requires at least one of `itemsToAdd` or `itemsToRemove` to be non-empty.
- HTTP 422 is used for token insufficiency (cannot process due to business rule violation).
- HTTP 409 is used for order state conflicts (order is acknowledged/ready/cancelled).
- All HTTP responses include proper content-type: application/json.
- Validation errors are returned before calling the domain UseCase.
- Token shortage details help the festival goer understand how to correct the request.
