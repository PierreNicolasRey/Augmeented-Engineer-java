# Application: Place a Group Order API Endpoint

**Context**
Festival goers need a REST API endpoint to create group orders by pooling their tokens with other festival goers. The endpoint must validate all contributors, their contributions, and invoke the Domain's `PlaceGroupOrderUseCase` to create the shared order.

**Problem**
The Application module requires a REST controller and DTOs to expose group order creation as an HTTP POST endpoint. Request validation must ensure valid contributors and contributions before delegating to the Domain.

**Acceptance Criteria**
- [ ] REST endpoint `POST /api/v1/orders/group` implemented in `OrderController`
- [ ] Request body accepts:
  - Array of contributor IDs (at least 2)
  - Array of order items (with type, subtype, quantity)
  - Optional: custom contributions per contributor per token type
  
- [ ] HTTP 201 response with created group order details:
  - orderId, status, items, total costs, contributor list with allocations
  
- [ ] HTTP 400 response for:
  - Less than 2 contributors
  - Empty order (no items)
  - Invalid item types/subtypes
  - Invalid quantities
  - Malformed request
  
- [ ] HTTP 404 response if any contributor festival goer not found
- [ ] HTTP 422 response if insufficient pooled tokens (with breakdown per contributor)
- [ ] HTTP 422 response if individual contribution exceeds available tokens
- [ ] DTOs use Java records for immutability
- [ ] Request DTO: `PlaceGroupOrderRequestDTO` with contributors, items, optional contributions
- [ ] Response DTO: `GroupOrderResponseDTO` with full allocation details
- [ ] Mappers convert DTOs to/from Domain models
- [ ] Application does NOT depend on Infrastructure module
- [ ] OpenAPI documentation fully describes all scenarios
- [ ] Integration tests cover happy path and all error cases

**Implementation Plan**
1. Create DTOs in `dto` package:
   
   - `ContributorRequestDTO` (record):
     - `contributorId: String`
     - `maxDrinkTokens: Integer` (optional, null means unlimited)
     - `maxSnackTokens: Integer` (optional, null means unlimited)
   
   - `GroupOrderItemRequestDTO` (record):
     - `itemType: String` (DRINK or FOOD)
     - `itemSubtype: String`
     - `quantity: int`
   
   - `PlaceGroupOrderRequestDTO` (record):
     - `contributors: List<ContributorRequestDTO>` (min 2)
     - `items: List<GroupOrderItemRequestDTO>` (min 1)
   
   - `ContributionResponseDTO` (record):
     - `contributorId: String`
     - `drinkTokensContributed: int`
     - `snackTokensContributed: int`
   
   - `GroupOrderResponseDTO` (record):
     - `orderId: String`
     - `status: String` (PENDING, ACKNOWLEDGED, READY, CANCELLED)
     - `items: List<OrderItemResponseDTO>`
     - `contributions: List<ContributionResponseDTO>`
     - `drinkTokensCost: int`
     - `snackTokensCost: int`
     - `createdAt: Instant`

2. Create `OrderController` methods (extend existing):
   - Endpoint: `POST /api/v1/orders/group`
   - Accepts: `PlaceGroupOrderRequestDTO`
   - Steps:
     a. Validate request (at least 2 contributors, non-empty items)
     b. Map request to Domain `PlaceGroupOrderRequest`
     c. Call `placeGroupOrderUseCase.execute(domainRequest)`
     d. Map result to `GroupOrderResponseDTO`
     e. Return HTTP 201 with location header
   - Exception handlers:
     - `InvalidGroupOrderException` → 400
     - `FestivalGoerNotFoundException` → 404 (specify which contributor)
     - `InsufficientGroupTokensException` → 422 with breakdown
     - `InvalidContributionException` → 422

3. Create `GroupOrderItemMapper` in `mapper` package:
   - Method: `fromRequestDTO(GroupOrderItemRequestDTO): OrderItem`
   - Method: `toResponseDTO(OrderItem): OrderItemResponseDTO`

4. Create `ContributionMapper` in `mapper` package:
   - Method: `toResponseDTO(TokenContribution): ContributionResponseDTO`

5. Create `GroupOrderMapper` in `mapper` package:
   - Method: `toResponseDTO(GroupOrder): GroupOrderResponseDTO`
   - Maps all contributions with their amounts

6. Create `GroupOrderRequestValidator`:
   - Validates contributors list has >= 2 entries
   - Validates items list not empty
   - Validates each item has valid type/subtype
   - Validates quantities > 0
   - Validates contributor IDs not duplicated

7. Create exception handler for group order errors:
   - `InsufficientGroupTokensException` returns:
     ```json
     {
       "error": "Insufficient pooled tokens",
       "requiredDrinkTokens": 5,
       "requiredSnackTokens": 3,
       "contributors": [
         {
           "contributorId": "fgv-001",
           "availableDrinkTokens": 2,
           "availableSnackTokens": 1
         },
         {
           "contributorId": "fgv-002",
           "availableDrinkTokens": 1,
           "availableSnackTokens": 1
         }
       ],
       "totalAvailableDrinkTokens": 3,
       "totalAvailableSnackTokens": 2
     }
     ```

8. Add OpenAPI annotations:
   - Document all success/error responses
   - Example request/response bodies
   - Field descriptions

9. Create integration tests:
   - Test successful group order with equal distribution
   - Test successful group order with proportional distribution
   - Test success with custom contributions
   - Test < 2 contributors rejection
   - Test empty items rejection
   - Test insufficient pooled tokens
   - Test individual contributor exceeding available
   - Test contributor not found
   - Test invalid item type/subtype
   - Test invalid quantity

**Gherkin Scenarios**
Feature: REST API - Place a Group Order

Scenario: Successfully place group order with 2 contributors
  Given festival goer "fgv-001" exists with 6 drink tokens, 0 reserved
  And festival goer "fgv-002" exists with 4 drink tokens, 0 reserved
  When a POST request is made to "/api/v1/orders/group" with:
    | Field | Value |
    | contributors[0].contributorId | fgv-001 |
    | contributors[1].contributorId | fgv-002 |
    | items[0].itemType | DRINK |
    | items[0].itemSubtype | NORMAL_ALCOHOLIC |
    | items[0].quantity | 2 |
  Then the response status is 201
  And the response contains orderId
  And the response contains status "PENDING"
  And the response contains 2 contributors with allocations
  And "fgv-001" allocated 1 drink token
  And "fgv-002" allocated 1 drink token

Scenario: Successfully place group order with 3 contributors
  Given festival goer "fgv-001" exists with 6 drink tokens
  And festival goer "fgv-002" exists with 3 drink tokens
  And festival goer "fgv-003" exists with 3 drink tokens
  When a POST request is made to "/api/v1/orders/group" with:
    | Field | Value |
    | contributors[0].contributorId | fgv-001 |
    | contributors[1].contributorId | fgv-002 |
    | contributors[2].contributorId | fgv-003 |
    | items[0].itemType | DRINK |
    | items[0].itemSubtype | NORMAL_ALCOHOLIC |
    | items[0].quantity | 6 |
  Then the response status is 201
  And the response contains 3 contributors with allocated amounts
  And total allocated = 6 drink tokens

Scenario: Successfully place group order with mixed items
  Given valid contributors with sufficient mixed tokens
  When placing a group order with drinks and food
  Then tokens are allocated per type (drink/snack) separately
  And response shows correct breakdown

Scenario: Successfully place group order with custom contributions
  Given festival goer "fgv-001" exists with 6 drink tokens
  And festival goer "fgv-002" exists with 4 drink tokens
  When a POST request is made to "/api/v1/orders/group" with:
    | Field | Value |
    | contributors[0].contributorId | fgv-001 |
    | contributors[0].maxDrinkTokens | 1 |
    | contributors[1].contributorId | fgv-002 |
    | contributors[1].maxDrinkTokens | 1 |
    | items[0].itemType | DRINK |
    | items[0].itemSubtype | NORMAL_ALCOHOLIC |
    | items[0].quantity | 2 |
  Then the response status is 201
  And "fgv-001" contributes exactly 1 drink token
  And "fgv-002" contributes exactly 1 drink token

Scenario: Less than 2 contributors returns 400
  Given festival goer "fgv-001" exists
  When a POST request is made to "/api/v1/orders/group" with:
    | Field | Value |
    | contributors[0].contributorId | fgv-001 |
    | items[0].itemType | DRINK |
    | items[0].itemSubtype | NON_ALCOHOLIC |
    | items[0].quantity | 1 |
  Then the response status is 400
  And the response contains error "At least 2 contributors required"

Scenario: Empty items returns 400
  Given festival goers "fgv-001" and "fgv-002" exist
  When a POST request is made to "/api/v1/orders/group" with:
    | Field | Value |
    | contributors[0].contributorId | fgv-001 |
    | contributors[1].contributorId | fgv-002 |
    | items | [] |
  Then the response status is 400
  And the response contains error "Order must contain at least 1 item"

Scenario: Insufficient pooled tokens returns 422
  Given festival goer "fgv-001" with 2 drink tokens
  And festival goer "fgv-002" with 1 drink token
  When placing a group order with 5 normal alcoholic drinks
  Then the response status is 422
  And the response contains requiredDrinkTokens: 5
  And the response contains totalAvailableDrinkTokens: 3
  And the response includes breakdown per contributor

Scenario: Contributor not found returns 404
  Given festival goer "fgv-001" exists
  And no festival goer "fgv-999" exists
  When a POST request is made to "/api/v1/orders/group" with:
    | Field | Value |
    | contributors[0].contributorId | fgv-001 |
    | contributors[1].contributorId | fgv-999 |
    | items[0].itemType | DRINK |
    | items[0].itemSubtype | NON_ALCOHOLIC |
    | items[0].quantity | 1 |
  Then the response status is 404
  And the response contains error "Festival goer 'fgv-999' not found"

Scenario: Duplicate contributors returns 400
  Given festival goers exist
  When a POST request is made to "/api/v1/orders/group" with:
    | Field | Value |
    | contributors[0].contributorId | fgv-001 |
    | contributors[1].contributorId | fgv-001 |
    | items[0].itemType | DRINK |
    | items[0].itemSubtype | NON_ALCOHOLIC |
    | items[0].quantity | 1 |
  Then the response status is 400
  And the response contains error "Duplicate contributors not allowed"

**Notes**
- This feature exposes the unified "Place a Group Order" capability from FEATURES.md feature 5.
- Application layer remains unaware of persistence details.
- All Domain models and exceptions are imported from Domain module.
- HTTP 201 with Location header follows REST conventions.
- Error responses include detailed breakdown of available tokens per contributor for transparency.
- Proportional distribution is default; custom contributions override this.
