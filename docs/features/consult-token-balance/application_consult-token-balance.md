# Application: Consult Token Balance API Endpoint

**Context**
Festival goers need a REST API endpoint to check their remaining token balance. The endpoint must return their current drink and snack token counts in a clear, user-friendly format.

**Problem**
The Application module requires a REST controller and DTO layer to expose the token balance inquiry as an HTTP GET endpoint, following the Hexagonal Architecture principle of unidirectional dependency (Application depends only on Domain).

**Acceptance Criteria**
- [ ] REST endpoint `GET /api/v1/festival-goers/{festivalGoerId}/tokens` implemented in `FestivalGoerController`
- [ ] Endpoint returns a `TokenBalanceDTO` with `drinkTokens` and `snackTokens`
- [ ] HTTP 200 response with token balance on success
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
   - Properties: `drinkTokens: int`, `snackTokens: int`
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
   - Converts Domain model to DTO
   
4. Add OpenAPI annotations to controller and DTO
   - Document success response (200)
   - Document error responses (404, 400)
   
5. Create integration tests
   - Test successful balance retrieval
   - Test festival goer not found scenario
   - Test invalid ID format

**Gherkin Scenarios**
Feature: REST API - Consult Token Balance

Scenario: Successfully retrieve token balance via API
  Given a festival goer with ID "fgv-001" exists
  And the festival goer has 5 drink tokens and 7 snack tokens
  When a GET request is made to "/api/v1/festival-goers/fgv-001/tokens"
  Then the response status is 200
  And the response contains drinkTokens: 5
  And the response contains snackTokens: 7

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

Scenario: Zero token balance is returned correctly
  Given a festival goer with ID "fgv-002" exists
  And the festival goer has 0 drink tokens and 0 snack tokens
  When a GET request is made to "/api/v1/festival-goers/fgv-002/tokens"
  Then the response status is 200
  And the response contains drinkTokens: 0
  And the response contains snackTokens: 0

**Notes**
- This ticket covers only the Application layer (REST exposure).
- The Application layer remains completely unaware of persistence or Infrastructure implementation details.
- `TokenQueryService` and `TokenBalance` are imported from the Domain module.
- Infrastructure module will wire `TokenQueryService` with the actual repository implementation.
- `FestivalGoerController` is intentionally designed as a single REST entry point for all festival goer-related operations. Future features (e.g., token transfer) will add endpoints to this same controller.
