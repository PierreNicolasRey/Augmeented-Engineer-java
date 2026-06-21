# Application: Validate Item Availability for Orders

**Context**
The Application layer exposes HTTP endpoints or handles REST requests related to item inventory management and status queries. For item availability validation during order placement, the Application layer remains transparent—it merely delegates to the Domain Use Cases (PlaceOrderUseCase, PlaceGroupOrderUseCase), which internally call the ItemAvailabilityService.

**Problem**
The Application layer must:
1. Handle HTTP requests for inventory status queries (optional, but useful for debugging/testing)
2. Map incoming order requests to Domain models correctly
3. Translate Domain exceptions (ItemNotFoundInCatalogException, InsufficientItemInventoryException) to meaningful HTTP responses
4. Return clear error messages to clients when item availability validation fails

**Acceptance Criteria**
- [ ] DTOs created for inventory queries (if needed):
  - `ItemInventoryStatusDto`: `itemId`, `totalQuantity`, `reservedQuantity`, `availableQuantity`
  - `CheckItemAvailabilityRequest`: list of items with requested quantities
  - `CheckItemAvailabilityResponse`: success/failure with detailed reason

- [ ] Exception mapping defined:
  - `ItemNotFoundInCatalogException` → HTTP 404 (Not Found)
  - `InsufficientItemInventoryException` → HTTP 400 (Bad Request) with detail: "Insufficient stock for item {itemId}: requested {requested}, available {available}"

- [ ] Optional: REST controller for inventory status queries
  - `GET /api/inventory/{itemId}` → returns `ItemInventoryStatusDto`
  - Useful for frontend/testing, not critical for core flow

- [ ] Existing PlaceOrderController and PlaceGroupOrderController mappers updated:
  - Order/GroupOrder DTOs to Domain models (already done)
  - No changes needed to controller logic—exception handling remains in Application layer

- [ ] Integration tests covering:
  - Order placement with valid items (happy path)
  - Order placement with invalid items (404, 400 responses)
  - HTTP error responses correctly formatted

**Implementation Plan**
1. Create DTOs in `application` module:
   - `ItemInventoryStatusDto` in `dto/inventory/` package
   - `CheckItemAvailabilityRequest` in `dto/inventory/` package
   - `CheckItemAvailabilityResponse` in `dto/inventory/` package

2. Create optional REST controller in `application` module:
   - `InventoryController` in `rest/inventory/` package
   - Method: `GET /api/inventory/{itemId}` → queries ItemInventoryRepository (via Domain) and returns status

3. Update `PlaceOrderController` and `PlaceGroupOrderController`:
   - Exception handling (Spring @ExceptionHandler or global exception handler):
     - Catch `ItemNotFoundInCatalogException` → HTTP 404
     - Catch `InsufficientItemInventoryException` → HTTP 400 with details

4. Global exception handler or per-controller handlers:
   - Map Domain exceptions to HTTP responses consistently

5. No changes to controller logic—validation occurs in Domain Use Cases

6. Integration tests:
   - Place order with valid items ✓
   - Place order with non-existent item ✓
   - Place order with insufficient stock ✓
   - Verify HTTP status codes and error messages

**Notes**
- The Application layer remains thin for this feature
- Exception handling may be centralized or per-controller depending on existing Application patterns
- DTOs for inventory queries are optional but useful for debugging
