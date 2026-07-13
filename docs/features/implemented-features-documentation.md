# Implemented Features Documentation

## Overview

| Feature | Status | Last Updated | Layer | Notes |
|---------|--------|--------------|-------|-------|
| Place Order | Implementing | 2026-06-21 | Application | REST endpoint + DTOs defined; Domain Use Case interface only; Infrastructure not started |
| Approve Order Change | Implementing | 2026-07-13 | Domain, Application | Pre-existing domain workflow plus newly added REST success-path contract; their port contract still needs alignment |

## Project State

The project currently contains partial implementations of the order-placement and order-change-approval flows. No infrastructure adapters or Spring dependency wiring are available for either flow, so the REST contracts are tested in isolation and are not yet runnable end to end.

Recent work introduced the application-layer contract for the first scenario of approving an acknowledged order change. It builds on the pre-existing domain implementation, which removes transferable prepared items, adds requested items, saves the changed order, and publishes an approval event. The custom TDD-cycle agent workflow was also refined and documented to preserve scenario context between its RED, GREEN, and REFACTOR phases.

---

## Feature: Place Order

### Summary

Allows festival goers to place group orders (drinks and food) with token-based payment validation. The feature handles order validation, token deduction tracking, and returns a complete order confirmation with cost breakdown.

### Status

- **Implementing:** Application layer complete (REST endpoint, error handling, 15 tests). Domain Use Case interface defined but not implemented. Infrastructure layer not started.

---

### Public API / Contracts

#### REST Endpoint

| Method | Path | Description | Status Codes |
|--------|------|-------------|--------------|
| POST | `/api/v1/orders` | Place a new group order | 201 Created, 400 Bad Request, 404 Not Found, 422 Unprocessable Entity |

**Request DTO:** [`PlaceOrderRequest`](../../application/src/main/java/com/exalt/it/belair/application/order/dto/PlaceOrderRequest.java)
- `festivalGoerId: String` — Unique identifier of the festival goer placing the order
- `items: List<OrderItemRequest>` — List of items to order

**Item Request DTO:** [`OrderItemRequest`](../../application/src/main/java/com/exalt/it/belair/application/order/dto/OrderItemRequest.java)
- `itemType: String` — Type of item: "DRINK" or "FOOD"
- `subtype: String` — Subtype: "NORMAL_ALCOHOLIC", "PREMIUM_ALCOHOLIC", "NON_ALCOHOLIC" (drinks) or "SNACK", "MEAL" (food)
- `quantity: Integer` — Number of items (must be > 0)

**Response DTO:** [`PlaceOrderResponse`](../../application/src/main/java/com/exalt/it/belair/application/order/dto/PlaceOrderResponse.java)
- `orderId: String` — Unique identifier of the created order
- `festivalGoerId: String` — ID of the festival goer
- `status: String` — Order status: "PENDING"
- `items: List<OrderItemResponse>` — Confirmed items in the order
- `drinkTokensCost: Integer` — Total drink tokens deducted
- `snackTokensCost: Integer` — Total snack tokens deducted

**Domain Port (Inbound):** [`PlaceOrderUseCase`](../../domain/src/main/java/com/exalt/it/belair/domain/order/ports/PlaceOrderUseCase.java)
- Interface: `Object execute(Object request)`
- Responsibility: Execute the business logic for placing an order (validation, token checking, order creation)

**Domain Exceptions (thrown by Use Case):**
- [`EmptyOrderException`](../../domain/src/main/java/com/exalt/it/belair/domain/order/exceptions/EmptyOrderException.java) — Order items list is empty (→ 400)
- [`InvalidItemTypeException`](../../domain/src/main/java/com/exalt/it/belair/domain/order/exceptions/InvalidItemTypeException.java) — Item type not in ["DRINK", "FOOD"] (→ 400)
- [`InvalidSubtypeException`](../../domain/src/main/java/com/exalt/it/belair/domain/order/exceptions/InvalidSubtypeException.java) — Item subtype invalid for its type (→ 400)
- [`InvalidQuantityException`](../../domain/src/main/java/com/exalt/it/belair/domain/order/exceptions/InvalidQuantityException.java) — Quantity ≤ 0 (→ 400)
- [`FestivalGoerNotFoundException`](../../domain/src/main/java/com/exalt/it/belair/domain/order/exceptions/FestivalGoerNotFoundException.java) — Festival goer ID does not exist (→ 404)
- [`InsufficientTokensException`](../../domain/src/main/java/com/exalt/it/belair/domain/order/exceptions/InsufficientTokensException.java) — Festival goer has insufficient tokens (→ 422)

**Error Handler:** [`GlobalErrorHandler`](../../application/src/main/java/com/exalt/it/belair/application/config/GlobalErrorHandler.java)
- Maps Domain exceptions to HTTP status codes and error response body
- Centralized error handling for the entire Application layer

---

### Quick Usage

#### Happy Path: Place a valid order

```bash
curl -X POST http://localhost:8080/api/v1/orders \
  -H "Content-Type: application/json" \
  -d '{
    "festivalGoerId": "fgv-001",
    "items": [
      {"itemType": "DRINK", "subtype": "NORMAL_ALCOHOLIC", "quantity": 2},
      {"itemType": "FOOD", "subtype": "SNACK", "quantity": 1}
    ]
  }'
```

**Response (201 Created):**
```json
{
  "orderId": "order-12345",
  "festivalGoerId": "fgv-001",
  "status": "PENDING",
  "items": [
    {"itemType": "DRINK", "subtype": "NORMAL_ALCOHOLIC", "quantity": 2},
    {"itemType": "FOOD", "subtype": "SNACK", "quantity": 1}
  ],
  "drinkTokensCost": 2,
  "snackTokensCost": 1
}
```

#### Error Case 1: Empty order list

```bash
curl -X POST http://localhost:8080/api/v1/orders \
  -H "Content-Type: application/json" \
  -d '{"festivalGoerId": "fgv-001", "items": []}'
```

**Response (400 Bad Request):**
```json
{"error": "Order items cannot be empty"}
```

#### Error Case 2: Insufficient tokens

```bash
curl -X POST http://localhost:8080/api/v1/orders \
  -H "Content-Type: application/json" \
  -d '{
    "festivalGoerId": "fgv-001",
    "items": [{"itemType": "DRINK", "subtype": "PREMIUM_ALCOHOLIC", "quantity": 100}]
  }'
```

**Response (422 Unprocessable Entity):**
```json
{"error": "Insufficient tokens"}
```

#### Error Case 3: Festival goer not found

```bash
curl -X POST http://localhost:8080/api/v1/orders \
  -H "Content-Type: application/json" \
  -d '{
    "festivalGoerId": "fgv-unknown",
    "items": [{"itemType": "DRINK", "subtype": "NON_ALCOHOLIC", "quantity": 1}]
  }'
```

**Response (404 Not Found):**
```json
{"error": "Festival goer fgv-unknown not found"}
```

---

### Design Decisions

**Token-based cost tracking:** Drinks and food have separate token costs per subtype (NORMAL_ALCOHOLIC=1, PREMIUM_ALCOHOLIC=2, SNACK=1, MEAL=3, etc.). Supports flexible pricing without coupling to payment systems.

**Exception-based error handling:** Domain Use Case throws specific exceptions. Application's `GlobalErrorHandler` maps these to HTTP status codes. Follows Hexagonal Architecture: Domain is pure business logic, Application handles HTTP contracts.

**Thin controller:** Controller only receives → delegates to Use Case → responds. All validation logic is in Domain Use Case, not in HTTP layer.

---

### Tests & Validation

**Test File:** [`PlaceOrderControllerTest.java`](../../application/src/test/java/com/exalt/it/belair/application/order/rest/PlaceOrderControllerTest.java)

**Coverage:** 15 tests via MockMvc
- 6 happy path tests (various drink/food combinations)
- 4 input validation tests (empty, invalid type, invalid subtype, invalid quantity)
- 1 resource not found test (festival goer doesn't exist)
- 4 business rule violation tests (insufficient tokens)

---

### Related Files

**Application Layer (Complete):**
- Controller: [`PlaceOrderController.java`](../../application/src/main/java/com/exalt/it/belair/application/order/rest/PlaceOrderController.java)
- DTOs: [`PlaceOrderRequest.java`](../../application/src/main/java/com/exalt/it/belair/application/order/dto/PlaceOrderRequest.java), [`PlaceOrderResponse.java`](../../application/src/main/java/com/exalt/it/belair/application/order/dto/PlaceOrderResponse.java), [`OrderItemRequest.java`](../../application/src/main/java/com/exalt/it/belair/application/order/dto/OrderItemRequest.java), [`OrderItemResponse.java`](../../application/src/main/java/com/exalt/it/belair/application/order/dto/OrderItemResponse.java)
- Error Handler: [`GlobalErrorHandler.java`](../../application/src/main/java/com/exalt/it/belair/application/config/GlobalErrorHandler.java)
- Tests: [`PlaceOrderControllerTest.java`](../../application/src/test/java/com/exalt/it/belair/application/order/rest/PlaceOrderControllerTest.java) (15 tests, all passing)

**Domain Layer (Partial):**
- Use Case Port: [`PlaceOrderUseCase.java`](../../domain/src/main/java/com/exalt/it/belair/domain/order/ports/PlaceOrderUseCase.java) (interface only, not implemented)
- Exceptions: [`EmptyOrderException.java`](../../domain/src/main/java/com/exalt/it/belair/domain/order/exceptions/EmptyOrderException.java), [`InvalidItemTypeException.java`](../../domain/src/main/java/com/exalt/it/belair/domain/order/exceptions/InvalidItemTypeException.java), [`InvalidSubtypeException.java`](../../domain/src/main/java/com/exalt/it/belair/domain/order/exceptions/InvalidSubtypeException.java), [`InvalidQuantityException.java`](../../domain/src/main/java/com/exalt/it/belair/domain/order/exceptions/InvalidQuantityException.java), [`FestivalGoerNotFoundException.java`](../../domain/src/main/java/com/exalt/it/belair/domain/order/exceptions/FestivalGoerNotFoundException.java), [`InsufficientTokensException.java`](../../domain/src/main/java/com/exalt/it/belair/domain/order/exceptions/InsufficientTokensException.java)

**Infrastructure Layer:** Not started yet

---

### Changelog

- `2026-06-21` — Initial implementation: Application layer only (REST controller, DTOs, error handler, 15 tests via MockMvc). Domain Use Case interface defined, 6 exceptions created.

---

### Notes

**Current Implementation Status:**
- ✅ Application layer: REST endpoint, 4 DTOs, error handler, 15 tests
- ⚠️ Domain layer: Use Case interface + 6 exceptions defined; Use Case implementation not started
- ❌ Infrastructure layer: Not started (persistence adapters, event handlers)

**Next Steps:**
1. Implement `PlaceOrderUseCase` in Domain layer (with full business logic and validation)
2. Add persistence adapters in Infrastructure layer (JPA repositories, mappers)
3. Add Domain event publishing for OrderCreatedEvent

---

## Feature: Approve Order Change

### Summary

Lets a bartender approve a requested change to an acknowledged order when every prepared item being removed can be transferred elsewhere. The newly added REST endpoint exposes the initial success response for the pre-existing domain workflow, which updates the order, stores it, and emits an approval event.

### Status

- **Implementing:** The first domain scenario and isolated application success-path contract are covered by tests. The domain implementation predates the application-layer addition and has not yet been adapted to [`ApproveOrderChangeUseCasePort`](../../domain/src/main/java/com/exalt/it/belair/domain/order/ports/in/ApproveOrderChangeUseCasePort.java): it currently returns `void`, while the new port returns an [`ApproveChangeReadModel`](../../domain/src/main/java/com/exalt/it/belair/domain/order/model/ApproveChangeReadModel.java). Dependency wiring and infrastructure adapters are also absent.

### Public API / Contracts

| Contract | Description |
|----------|-------------|
| `POST /api/v1/orders/{orderId}/changes/approve` | Approves an order-change request. The bartender identity is provided by the required `X-Bartender-Id` header. Returns `200 OK` for the currently covered success case. |
| [`ApproveChangeResponseDTO`](../../application/src/main/java/com/exalt/it/belair/application/dto/ApproveChangeResponseDTO.java) | Response containing the order ID, status, revised readiness timestamp and duration, message, and approval timestamp. |
| [`ApproveOrderChangeUseCasePort`](../../domain/src/main/java/com/exalt/it/belair/domain/order/ports/in/ApproveOrderChangeUseCasePort.java) | Intended inbound contract. It accepts an order ID and bartender ID and returns an `ApproveChangeReadModel`. |
| [`OrderChangeApprovedEvent`](../../domain/src/main/java/com/exalt/it/belair/domain/order/events/OrderChangeApprovedEvent.java) | Domain event published after the order update is saved. |

### Quick Usage

```bash
curl -X POST http://localhost:8080/api/v1/orders/ord-001/changes/approve \
  -H "X-Bartender-Id: bartender-001"
```

**Expected response shape (200 OK):**

```json
{
  "orderId": "ord-001",
  "status": "ACKNOWLEDGED",
  "newEstimatedReadinessAt": "2026-07-13T15:48:28",
  "newEstimatedReadinessMinutes": 60,
  "message": "Change approved",
  "approvedAt": "2026-07-13T14:48:28"
}
```

The response illustrates the API contract tested in isolation. Until the pre-existing domain implementation is aligned with the port and wired, the endpoint is not available in a running application.

### Design Decisions

**Dedicated command use case:** Approval changes order state and publishes a domain event, so it is modelled as [`ApproveOrderChangeUseCase`](../../domain/src/main/java/com/exalt/it/belair/domain/order/usecases/ApproveOrderChangeUseCase.java), not a query service. This follows the project's CQS-infused hexagonal architecture.

**Transfer validation before mutation:** The use case checks each prepared item requested for removal through [`IItemTransferService`](../../domain/src/main/java/com/exalt/it/belair/domain/order/ports/out/IItemTransferService.java) before updating the order. This prevents a partially applied change when a prepared item cannot be reassigned.

**Read-model REST boundary:** The controller maps the intended domain read model to an application DTO through [`ApproveChangeResponseMapper`](../../application/src/main/java/com/exalt/it/belair/application/mapper/ApproveChangeResponseMapper.java), keeping domain objects out of the HTTP response.

### Tests & Validation

| Test | Covered scenario |
|------|------------------|
| [`ApproveOrderChangeUseCaseTest`](../../domain/src/test/java/com/exalt/it/belair/domain/order/usecases/ApproveOrderChangeUseCaseTest.java) | Removes one transferable prepared item, adds one requested item, stores the updated acknowledged order, assigns a readiness timestamp, and publishes the approval event. |
| [`ApproveOrderChangeControllerTest`](../../application/src/test/java/com/exalt/it/belair/application/order/rest/ApproveOrderChangeControllerTest.java) | Maps a successful port response to `200 OK` and serializes every response field. |

The not-found, non-transferable-item, rejection, readiness-estimation, and end-to-end wiring scenarios have not yet been implemented.

### Related Files

**Application:**
- Controller: [`ApproveOrderChangeController.java`](../../application/src/main/java/com/exalt/it/belair/application/rest/order/ApproveOrderChangeController.java)
- Response DTO: [`ApproveChangeResponseDTO.java`](../../application/src/main/java/com/exalt/it/belair/application/dto/ApproveChangeResponseDTO.java)
- Mapper: [`ApproveChangeResponseMapper.java`](../../application/src/main/java/com/exalt/it/belair/application/mapper/ApproveChangeResponseMapper.java)

**Domain:**
- Use case: [`ApproveOrderChangeUseCase.java`](../../domain/src/main/java/com/exalt/it/belair/domain/order/usecases/ApproveOrderChangeUseCase.java)
- Inbound port: [`ApproveOrderChangeUseCasePort.java`](../../domain/src/main/java/com/exalt/it/belair/domain/order/ports/in/ApproveOrderChangeUseCasePort.java)
- Read model: [`ApproveChangeReadModel.java`](../../domain/src/main/java/com/exalt/it/belair/domain/order/model/ApproveChangeReadModel.java)
- Event: [`OrderChangeApprovedEvent.java`](../../domain/src/main/java/com/exalt/it/belair/domain/order/events/OrderChangeApprovedEvent.java)

**Infrastructure:** Not implemented.

### Changelog

- `2026-07-13` — Documented the new application contract for the pre-existing approval-change workflow, its test coverage, and the remaining port-alignment work.

### Notes

The existing use case sets the readiness time to the current time; it does not yet calculate a revised readiness estimate. Its temporary `IllegalArgumentException` and `IllegalStateException` failures still need feature-specific domain exceptions and HTTP error mappings.
