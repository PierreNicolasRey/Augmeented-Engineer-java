# Application: Consult Token Balance API Endpoint

**Context**
Festival goers need a REST API endpoint to check their remaining token balance. The endpoint must return their current drink and snack token counts in a clear, user-friendly format.

**Problem**
The Application module requires a REST controller and DTO layer to expose the token balance inquiry as an HTTP GET endpoint, following the Hexagonal Architecture principle of unidirectional dependency (Application depends only on Domain).

**Acceptance Criteria**
- [ ] REST endpoint `GET /api/v1/festival-goers/{festivalGoerId}/tokens` implemented in `FestivalGoerController`
- [ ] Endpoint returns a `TokenBalanceDTO` with:
  - `totalDrinkTokens`, `reservedDrinkTokens`, `availableDrinkTokens`
  - `totalSnackTokens`, `reservedSnackTokens`, `availableSnackTokens`
- [ ] HTTP 200 response with token balance breakdown on success
- [ ] HTTP 404 response with error message if festival goer not found
- [ ] HTTP 400 response for invalid festival goer ID format
- [ ] Endpoint mapped to `TokenQueryService.consultTokenBalance()`
- [ ] DTOs use Java records for immutability
- [ ] Application does NOT depend on Infrastructure module
- [ ] OpenAPI documentation generated for the endpoint
- [ ] Integration tests verify happy path and error cases
- [ ] `FestivalGoerController` designed to accommodate other festival goer operations (e.g., token transfer)

**Implementation Plan**
1. Create `TokenBalanceDTO` record in `dto` package
   - Properties:
     - `totalDrinkTokens: int`, `reservedDrinkTokens: int`, `availableDrinkTokens: int`
     - `totalSnackTokens: int`, `reservedSnackTokens: int`, `availableSnackTokens: int`
   - Use `@Schema` annotations for OpenAPI documentation
   
2. Create `FestivalGoerController` in `rest` package
   - Inject `TokenQueryService` from Domain
   - Endpoint: `GET /api/v1/festival-goers/{festivalGoerId}/tokens`
   - Validate path variable `festivalGoerId`
   - Call `tokenQueryService.consultTokenBalance(festivalGoerId)`
   - Map result to `TokenBalanceDTO`
   - Handle `FestivalGoerNotFoundException` → HTTP 404
   - Document controller as the single entry point for all festival goer operations
   
3. Create `TokenBalanceMapper` in `mapper` package
   - Method: `toDTO(TokenBalance): TokenBalanceDTO`
   - Converts Domain model to DTO (includes total, reserved, available for both types)
   
4. Add OpenAPI annotations to controller and DTO
   - Document success response (200) with all token fields
   - Document error responses (404, 400)
   
5. Create integration tests
   - Test successful balance retrieval with no reservations
   - Test balance with partial reservations
   - Test balance with all tokens reserved
   - Test festival goer not found scenario
   - Test invalid ID format

**Gherkin Scenarios**
Feature: REST API - Consult Token Balance

Scenario: Successfully retrieve token balance with no reservations
  Given a festival goer with ID "fgv-001" exists
  And the festival goer has 6 total drink tokens, 0 reserved, 9 total snack tokens, 0 reserved
  When a GET request is made to "/api/v1/festival-goers/fgv-001/tokens"
  Then the response status is 200
  And the response contains totalDrinkTokens: 6
  And the response contains reservedDrinkTokens: 0
  And the response contains availableDrinkTokens: 6
  And the response contains totalSnackTokens: 9
  And the response contains reservedSnackTokens: 0
  And the response contains availableSnackTokens: 9

Scenario: Successfully retrieve token balance with partial drink reservation
  Given a festival goer with ID "fgv-001" exists
  And the festival goer has 6 total drink tokens with 2 reserved, 9 total snack tokens with 0 reserved
  When a GET request is made to "/api/v1/festival-goers/fgv-001/tokens"
  Then the response status is 200
  And the response contains totalDrinkTokens: 6
  And the response contains reservedDrinkTokens: 2
  And the response contains availableDrinkTokens: 4
  And the response contains totalSnackTokens: 9
  And the response contains availableSnackTokens: 9

Scenario: Successfully retrieve token balance with multiple reservations
  Given a festival goer with ID "fgv-001" exists
  And the festival goer has 6 total drink tokens with 3 reserved, 9 total snack tokens with 4 reserved
  When a GET request is made to "/api/v1/festival-goers/fgv-001/tokens"
  Then the response status is 200
  And the response contains totalDrinkTokens: 6
  And the response contains reservedDrinkTokens: 3
  And the response contains availableDrinkTokens: 3
  And the response contains totalSnackTokens: 9
  And the response contains reservedSnackTokens: 4
  And the response contains availableSnackTokens: 5

Scenario: Successfully retrieve token balance with all tokens reserved
  Given a festival goer with ID "fgv-001" exists
  And the festival goer has 6 total drink tokens with 6 reserved, 9 total snack tokens with 9 reserved
  When a GET request is made to "/api/v1/festival-goers/fgv-001/tokens"
  Then the response status is 200
  And the response contains availableDrinkTokens: 0
  And the response contains availableSnackTokens: 0

Scenario: Festival goer not found returns 404
  Given no festival goer with ID "fgv-999" exists
  When a GET request is made to "/api/v1/festival-goers/fgv-999/tokens"
  Then the response status is 404
  And the response contains error message "Festival goer not found"

Scenario: Invalid festival goer ID format returns 400
  Given an invalid festival goer ID format "invalid-id-123"
  When a GET request is made to "/api/v1/festival-goers/invalid-id-123/tokens"
  Then the response status is 400
  And the response contains error message about invalid format

**Notes**
- This ticket covers only the Application layer (REST exposure).
- The Application layer remains completely unaware of persistence or Infrastructure implementation details.
- `TokenQueryService` and `TokenBalance` are imported from the Domain module.
- Infrastructure module will wire `TokenQueryService` with the actual repository implementation.
- `FestivalGoerController` is intentionally designed as a single REST entry point for all festival goer-related operations. Future features (e.g., token transfer) will add endpoints to this same controller.
