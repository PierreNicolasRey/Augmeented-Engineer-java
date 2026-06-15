# Application: Place an Order API Endpoint

**Context**
Festival goers need a REST API endpoint to create orders. This unified feature covers ordering:
- Drinks (non-alcoholic, normal alcoholic, premium alcoholic)
- Food (snacks and meals)
- Multiple items in a single order in one transaction

The endpoint must validate all inputs, invoke the Domain's `PlaceOrderUseCase`, and return the created order details.

**Problem**
The Application module requires a REST controller and DTOs to expose order creation as an HTTP POST endpoint. Input validation must ensure valid item combinations and quantities before delegating to the Domain.

**Acceptance Criteria**
- [ ] REST endpoint `POST /api/v1/orders` implemented in `OrderController`
- [ ] Request body accepts array of order items with type, subtype, and quantity
- [ ] HTTP 201 response with created order details (orderId, status, cost breakdown)
- [ ] HTTP 400 response for:
  - Empty order (no items)
  - Invalid item types/subtypes
  - Invalid quantities (zero or negative)
  - Malformed request
- [ ] HTTP 404 response if festival goer not found
- [ ] HTTP 422 response if insufficient tokens (with breakdown of required vs available)
- [ ] DTOs use Java records for immutability
- [ ] Request DTO: `PlaceOrderRequestDTO` with list of `OrderItemRequestDTO`
- [ ] Response DTO: `OrderResponseDTO` with orderId, status, items, cost breakdown
- [ ] Mappers convert DTOs to/from Domain models
- [ ] Application does NOT depend on Infrastructure module
- [ ] OpenAPI documentation fully describes all scenarios
- [ ] Integration tests cover happy path and all error cases

**Implementation Plan**
1. Create DTOs in `dto` package:
   - `OrderItemRequestDTO` (record):
     - `itemType: String` (DRINK or FOOD)
     - `itemSubtype: String` (NON_ALCOHOLIC, NORMAL_ALCOHOLIC, PREMIUM_ALCOHOLIC, SNACK, MEAL)
     - `quantity: int`
   
   - `PlaceOrderRequestDTO` (record):
     - `festivalGoerId: String`
     - `items: List<OrderItemRequestDTO>`
     - Validation: items list not empty, each item has valid subtype
   
   - `OrderItemResponseDTO` (record):
     - `itemType: String`
     - `itemSubtype: String`
     - `quantity: int`
     - `costInTokens: int`
   
   - `OrderResponseDTO` (record):
     - `orderId: String`
     - `festivalGoerId: String`
     - `status: String` (PENDING, ACKNOWLEDGED, READY, CANCELLED)
     - `items: List<OrderItemResponseDTO>`
     - `drinkTokensCost: int`
     - `snackTokensCost: int`
     - `createdAt: Instant`

2. Create `OrderController` in `rest` package:
   - Inject: `PlaceOrderUseCase` from Domain
   - Endpoint: `POST /api/v1/orders`
   - Accepts: `PlaceOrderRequestDTO`
   - Steps:
     a. Validate request (not null, non-empty items)
     b. Map request to Domain `PlaceOrderRequest`
     c. Call `placeOrderUseCase.execute(domainRequest)`
     d. Map result to `OrderResponseDTO`
     e. Return HTTP 201 with location header
   - Exception handlers:
     - `FestivalGoerNotFoundException` → 404
     - `InsufficientTokensException` → 422 with token breakdown
     - `InvalidOrderException` → 400
     - Generic validation errors → 400

3. Create `OrderItemMapper` in `mapper` package:
   - Method: `fromRequestDTO(OrderItemRequestDTO): OrderItem`
   - Method: `toResponseDTO(OrderItem): OrderItemResponseDTO`
   - Validates subtype matches item type
   - Throws `InvalidItemTypeException` if mismatch

4. Create `OrderMapper` in `mapper` package:
   - Method: `toResponseDTO(Order): OrderResponseDTO`
   - Converts Domain order to REST response
   - Calculates timestamps
   - Formats all enums to string

5. Create `PlaceOrderRequestValidator`:
   - Validates items list not empty
   - Validates each item has quantity > 0
   - Validates itemSubtype matches itemType
   - Valid combinations:
     - DRINK: NON_ALCOHOLIC, NORMAL_ALCOHOLIC, PREMIUM_ALCOHOLIC
     - FOOD: SNACK, MEAL

6. Create exception handler for order-related errors:
   - `InsufficientTokensException` returns:
     ```json
     {
       "error": "Insufficient tokens",
       "requiredDrinkTokens": 5,
       "availableDrinkTokens": 2,
       "requiredSnackTokens": 3,
       "availableSnackTokens": 9
     }
     ```

7. Add OpenAPI annotations:
   - Document all success/error responses
   - Example request/response bodies
   - Field descriptions for item types/subtypes

8. Create integration tests:
   - Test successful order placement (drinks only)
   - Test successful order placement (food only)
   - Test successful order placement (mixed items)
   - Test insufficient drink tokens
   - Test insufficient snack tokens
   - Test insufficient both token types
   - Test empty order rejection
   - Test invalid item type/subtype rejection
   - Test invalid quantity rejection
   - Test festival goer not found

**Gherkin Scenarios**
Feature: REST API - Place an Order

Scenario: Successfully place order with single drink
  Given a festival goer with ID "fgv-001" exists
  And the festival goer has 6 drink tokens and 9 snack tokens
  When a POST request is made to "/api/v1/orders" with:
    | Field | Value |
    | festivalGoerId | fgv-001 |
    | items[0].itemType | DRINK |
    | items[0].itemSubtype | NORMAL_ALCOHOLIC |
    | items[0].quantity | 1 |
  Then the response status is 201
  And the response contains orderId
  And the response contains status "PENDING"
  And the response contains drinkTokensCost: 1
  And the response contains snackTokensCost: 0
  And the Location header points to the created order

Scenario: Successfully place order with non-alcoholic drinks
  Given a festival goer with ID "fgv-001" exists
  And the festival goer has 6 drink tokens and 9 snack tokens
  When a POST request is made to "/api/v1/orders" with:
    | Field | Value |
    | festivalGoerId | fgv-001 |
    | items[0].itemType | DRINK |
    | items[0].itemSubtype | NON_ALCOHOLIC |
    | items[0].quantity | 3 |
  Then the response status is 201
  And the response contains drinkTokensCost: 0
  And the response contains 3 drink items

Scenario: Successfully place order with premium drinks
  Given a festival goer with ID "fgv-001" exists
  And the festival goer has 6 drink tokens and 9 snack tokens
  When a POST request is made to "/api/v1/orders" with:
    | Field | Value |
    | festivalGoerId | fgv-001 |
    | items[0].itemType | DRINK |
    | items[0].itemSubtype | PREMIUM_ALCOHOLIC |
    | items[0].quantity | 2 |
  Then the response status is 201
  And the response contains drinkTokensCost: 4

Scenario: Successfully place order with snacks
  Given a festival goer with ID "fgv-001" exists
  And the festival goer has 6 drink tokens and 9 snack tokens
  When a POST request is made to "/api/v1/orders" with:
    | Field | Value |
    | festivalGoerId | fgv-001 |
    | items[0].itemType | FOOD |
    | items[0].itemSubtype | SNACK |
    | items[0].quantity | 2 |
  Then the response status is 201
  And the response contains snackTokensCost: 2
  And the response contains 2 food items

Scenario: Successfully place order with meals
  Given a festival goer with ID "fgv-001" exists
  And the festival goer has 6 drink tokens and 9 snack tokens
  When a POST request is made to "/api/v1/orders" with:
    | Field | Value |
    | festivalGoerId | fgv-001 |
    | items[0].itemType | FOOD |
    | items[0].itemSubtype | MEAL |
    | items[0].quantity | 2 |
  Then the response status is 201
  And the response contains snackTokensCost: 6

Scenario: Successfully place complex mixed order
  Given a festival goer with ID "fgv-001" exists
  And the festival goer has 6 drink tokens and 9 snack tokens
  When a POST request is made to "/api/v1/orders" with:
    | Field | Value |
    | festivalGoerId | fgv-001 |
    | items[0].itemType | DRINK |
    | items[0].itemSubtype | NORMAL_ALCOHOLIC |
    | items[0].quantity | 2 |
    | items[1].itemType | DRINK |
    | items[1].itemSubtype | NON_ALCOHOLIC |
    | items[1].quantity | 1 |
    | items[2].itemType | FOOD |
    | items[2].itemSubtype | SNACK |
    | items[2].quantity | 2 |
    | items[3].itemType | FOOD |
    | items[3].itemSubtype | MEAL |
    | items[3].quantity | 1 |
  Then the response status is 201
  And the response contains drinkTokensCost: 2
  And the response contains snackTokensCost: 5
  And the response contains 4 order items

Scenario: Empty order is rejected
  Given a festival goer with ID "fgv-001" exists
  When a POST request is made to "/api/v1/orders" with:
    | Field | Value |
    | festivalGoerId | fgv-001 |
    | items | [] |
  Then the response status is 400
  And the response contains error "Order must contain at least 1 item"

Scenario: Invalid item type is rejected
  Given a festival goer with ID "fgv-001" exists
  When a POST request is made to "/api/v1/orders" with:
    | Field | Value |
    | festivalGoerId | fgv-001 |
    | items[0].itemType | INVALID |
    | items[0].itemSubtype | SOMETHING |
    | items[0].quantity | 1 |
  Then the response status is 400
  And the response contains error about invalid item type

Scenario: Invalid subtype for item type is rejected
  Given a festival goer with ID "fgv-001" exists
  When a POST request is made to "/api/v1/orders" with:
    | Field | Value |
    | festivalGoerId | fgv-001 |
    | items[0].itemType | DRINK |
    | items[0].itemSubtype | SNACK |
    | items[0].quantity | 1 |
  Then the response status is 400
  And the response contains error "SNACK subtype is invalid for DRINK type"

Scenario: Zero quantity is rejected
  Given a festival goer with ID "fgv-001" exists
  When a POST request is made to "/api/v1/orders" with:
    | Field | Value |
    | festivalGoerId | fgv-001 |
    | items[0].itemType | DRINK |
    | items[0].itemSubtype | NORMAL_ALCOHOLIC |
    | items[0].quantity | 0 |
  Then the response status is 400
  And the response contains error "Quantity must be greater than 0"

Scenario: Negative quantity is rejected
  Given a festival goer with ID "fgv-001" exists
  When a POST request is made to "/api/v1/orders" with:
    | Field | Value |
    | festivalGoerId | fgv-001 |
    | items[0].itemType | DRINK |
    | items[0].itemSubtype | NORMAL_ALCOHOLIC |
    | items[0].quantity | -1 |
  Then the response status is 400

Scenario: Festival goer not found returns 404
  Given no festival goer with ID "fgv-999" exists
  When a POST request is made to "/api/v1/orders" with:
    | Field | Value |
    | festivalGoerId | fgv-999 |
    | items[0].itemType | DRINK |
    | items[0].itemSubtype | NON_ALCOHOLIC |
    | items[0].quantity | 1 |
  Then the response status is 404
  And the response contains error "Festival goer not found"

Scenario: Insufficient drink tokens returns 422
  Given a festival goer with ID "fgv-001" exists
  And the festival goer has 1 drink token and 9 snack tokens
  When a POST request is made to "/api/v1/orders" with:
    | Field | Value |
    | festivalGoerId | fgv-001 |
    | items[0].itemType | DRINK |
    | items[0].itemSubtype | NORMAL_ALCOHOLIC |
    | items[0].quantity | 3 |
  Then the response status is 422
  And the response contains requiredDrinkTokens: 3
  And the response contains availableDrinkTokens: 1

Scenario: Insufficient snack tokens returns 422
  Given a festival goer with ID "fgv-001" exists
  And the festival goer has 6 drink tokens and 2 snack tokens
  When a POST request is made to "/api/v1/orders" with:
    | Field | Value |
    | festivalGoerId | fgv-001 |
    | items[0].itemType | FOOD |
    | items[0].itemSubtype | MEAL |
    | items[0].quantity | 3 |
  Then the response status is 422
  And the response contains requiredSnackTokens: 9
  And the response contains availableSnackTokens: 2

Scenario: Insufficient both token types returns 422
  Given a festival goer with ID "fgv-001" exists
  And the festival goer has 0 drink tokens and 1 snack token
  When a POST request is made to "/api/v1/orders" with:
    | Field | Value |
    | festivalGoerId | fgv-001 |
    | items[0].itemType | DRINK |
    | items[0].itemSubtype | PREMIUM_ALCOHOLIC |
    | items[0].quantity | 1 |
    | items[1].itemType | FOOD |
    | items[1].itemSubtype | MEAL |
    | items[1].quantity | 1 |
  Then the response status is 422
  And the response contains requiredDrinkTokens: 2
  And the response contains availableDrinkTokens: 0
  And the response contains requiredSnackTokens: 3
  And the response contains availableSnackTokens: 1

**Notes**
- This feature mutualizes features 2, 3, and 4 from FEATURES.md.
- Application layer remains completely unaware of persistence details.
- All Domain models and exceptions are imported from Domain module.
- Infrastructure module will wire `PlaceOrderUseCase` with actual repository implementations.
- HTTP 201 (Created) status with Location header is industry standard for resource creation.
- Error responses include detailed breakdown of token requirements vs availability for user feedback.
