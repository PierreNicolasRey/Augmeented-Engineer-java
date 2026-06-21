# Implemented Features Documentation

## Overview

| Feature | Status | Last Updated | Layer | Notes |
|---------|--------|--------------|-------|-------|
| Place Order | Implementing | 2026-06-21 | Application | REST endpoint + DTOs defined; Domain Use Case interface only; Infrastructure not started |

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
