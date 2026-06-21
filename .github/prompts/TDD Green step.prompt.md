---
agent: agent
name: TDD Green step
description: This prompt is used to implement minimal production code to make a failing RED test pass in a TDD workflow for an AI agent
argument-hint: Implement the following test scenario to make it pass with minimal logic: {test_file} - {test_method}
tools: ['execute/getTerminalOutput', 'execute/runInTerminal', 'read/problems', 'read/readFile', 'read/terminalSelection', 'read/terminalLastCommand', 'edit/createDirectory', 'edit/createFile', 'edit/editFiles', 'search', 'upstash/context7/*', 'todo']
model: Claude Haiku 4.5 (copilot)
---

# Green TDD step prompt

## Instructions

**ABSOLUTE RULE: Implement ONLY what is necessary to make the test pass. No more. No anticipation. Period.**

### Philosophy: "TDD as if you meant it"
During GREEN, we implement code **in a way that makes the test pass with minimal logic**. We embrace:
- Literal returns (return hardcoded values if that's what passes the test)
- Duplication (copy-paste code rather than extract prematurely)
- No premature refactoring or design patterns
- No interfaces, repositories, or abstractions unless the test requires them
- The goal is to **make the test pass**, not to build production code yet. The REFACTOR phase handles extraction and design.

## Input

Provide:
1. **Test file path** (workspace-relative): `domain/src/test/java/com/it/exalt/belair/domain/order/usecases/PlaceOrderUseCaseTest.java`
2. **Test method name**: `placeOrder_shouldCreateOrderWithPendingStatus_whenPlacingOrderWithSingleNormalAlcoholicDrink`
3. **Scenario context** (from the domain ticket or specification)

## Process

1. **Read the failing test carefully:**
   - Understand exactly what inputs are passed.
   - Understand exactly what outputs/assertions are expected.
   - **Your job is to make THIS test pass, not to predict future tests.**

2. **Trace the test execution path:**
   - Start from the test's WHEN clause.
   - Follow each method call into production code.
   - Identify all methods that must exist and return something specific.

3. **Implement with minimal logic:**
   - Start with hardcoded returns if that passes the test.
   - Add minimal conditionals or loops only if the test requires branching.
   - **Do NOT create helper methods, extractors, or builders.**
   - **Do NOT add validation unless the test explicitly calls it.**
   - **Do NOT create domain events, repositories, or ports unless the test uses them.**

4. **Create fakes/stubs ONLY if required:**
   - If the test passes an object to production code, implement that object minimally.
   - Only add fields and methods that the test references.
   - Fakes remain in `src/test/java`; they do NOT become production interfaces yet.

5. **Run the test to confirm it passes:**
   - Execute the test. It **MUST pass**.
   - All assertions in the test must succeed.
   - No test modifications are allowed.

## Requirements

**Zero tolerance for over-engineering. These rules are non-negotiable.**

- **ABSOLUTE: Implement code that makes the test pass, using the simplest possible logic.**
  - Hardcoded returns are acceptable if they pass the test.
  - Example: If test expects `order.getStatus() == PENDING`, return `PENDING` directly. Do NOT compute it from state.
  - Example: If test expects `order.getItemCount() == 1`, return `1` directly. Do NOT count items in a list.

- **ABSOLUTE: No production interfaces, repositories, or event publishers during GREEN.**
  - If the test doesn't instantiate or inject a repository, do NOT create one.
  - If the test doesn't call `eventPublisher.publish()`, do NOT create an event publisher.
  - Stay literal to what the test does.
  - *Exception*: If RED created a port skeleton and the test directly uses it (e.g., calls a method on an injected fake), implement that method minimally.

- **ABSOLUTE: No additional methods, getters, or fields beyond what the test uses.**
  - Example: Do NOT add `reserve()`, `deduct()`, or helper methods the test doesn't call.
  - Example: Do NOT add a `List<OrderItem> items` field if the test doesn't inspect it.

- **ABSOLUTE: No builder patterns, factory methods, or design patterns.**
  - Constructors should accept what the test passes.
  - Factory methods (like `createDrinkItem()`) should return the simplest possible object.
  - No intermediate DTO or value object transformations.

- **ABSOLUTE: Implement the simplest version that passes the test.**
  - If a test passes a drink type and quantity, store them as-is. Do NOT compute costs yet unless the test calls a getter.
  - If a test calls `getDrinkTokenCost()`, calculate it in that method only.
  - Do NOT precompute or cache results.

- **ABSOLUTE: Do NOT modify the test or any other tests in the class.**
  - The test is the specification. Treat it as immutable.
  - If the test seems wrong, flag it and ask (use ⚠️ emoji), but do not modify it.

- **ABSOLUTE: Follow the module testing guidelines** (domain-testing-guidelines.md, etc.) for assertions and patterns.

- **ABSOLUTE: The test MUST pass when executed.**
  - If the test still fails, keep implementing until it passes.
  - If assertions still fail, analyze what's missing and add minimal code.

## Examples

### Domain test example: Place Order scenario (ultra-minimal GREEN)

**Input:**
- Test file: `domain/src/test/java/com/it/exalt/belair/domain/order/usecases/PlaceOrderUseCaseTest.java`
- Test method: `placeOrder_shouldCreateOrderWithPendingStatus_whenPlacingOrderWithSingleNormalAlcoholicDrink`
- Scenario: Place order with 1 normal alcoholic drink (cost 1 token)

**Test code** (already written in RED):
```java
@Test
void placeOrder_shouldCreateOrderWithPendingStatus_whenPlacingOrderWithSingleNormalAlcoholicDrink() {
    String festivalGoerId = "fgv-001";
    int drinkTokens = 6;
    int snackTokens = 9;
    
    sut = new PlaceOrderUseCase();
    var item = OrderItem.createDrinkItem(DrinkType.NORMAL_ALCOHOLIC, 1);
    Order order = sut.placeOrder(festivalGoerId, item, drinkTokens, snackTokens);
    
    assertThat(order).isNotNull();
    assertThat(order.getStatus()).isEqualTo(OrderStatus.PENDING);
    assertThat(order.getItemCount()).isEqualTo(1);
    assertThat(order.getDrinkTokenCost()).isEqualTo(1);
}
```

**Expected Output in GREEN phase: Minimal implementations only.**

**1. Implement OrderItem factory method:**
```java
public record OrderItem(DrinkType drinkType, int quantity) {
    public static OrderItem createDrinkItem(DrinkType type, int quantity) {
        return new OrderItem(type, quantity);  // Minimal: just create the record
    }
}
```

**2. Implement Order class minimally:**
```java
public class Order {
    private OrderStatus status = OrderStatus.PENDING;  // Hardcoded for this test
    private OrderItem item;  // Store what we receive
    
    public Order(OrderItem item) {
        this.item = item;
    }
    
    public OrderStatus getStatus() {
        return OrderStatus.PENDING;  // Hardcoded: the test expects PENDING
    }

    public int getItemCount() {
        return 1;  // Hardcoded: the test expects 1 item
    }

    public int getDrinkTokenCost() {
        // Minimal logic: if it's a normal alcoholic drink, cost is 1
        if (item.drinkType() == DrinkType.NORMAL_ALCOHOLIC) {
            return 1;
        }
        return 0;
    }
}
```

**3. Implement PlaceOrderUseCase minimally:**
```java
public class PlaceOrderUseCase {
    public Order placeOrder(String festivalGoerId, OrderItem item, int drinkTokens, int snackTokens) {
        return new Order(item);  // Create and return the order immediately
    }
}
```

**What is NOT done in GREEN for this test:**
- ❌ No token reservation logic (test doesn't assert it)
- ❌ No `FestivalGoer` or `FestivalGoerBalance` models (test passes raw ints)
- ❌ No `OrderRepository` persistence (test doesn't check if order was saved)
- ❌ No `EventPublisherPort` (test doesn't assert events were published)
- ❌ No `OrderId` value object (not needed to pass this test)
- ❌ No `FoodType` or meal handling (only one drink type tested)
- ❌ No validation of token sufficiency (test doesn't trigger an error path)
- ❌ No builder or factory patterns beyond `createDrinkItem()`
- ❌ No calculation of total snack tokens (test doesn't check it)
- ❌ No state transition logic (only PENDING status tested)

**Execution result:**
- Test compiles ✓
- Test runs and passes ✓
- All assertions succeed ✓
- Zero over-engineering ✓

---

## Negative Examples (What NOT to do)

### ❌ WRONG: Creating a token reservation system prematurely
```java
// WRONG - The test doesn't check this
public Order placeOrder(String festivalGoerId, OrderItem item, int drinkTokens, int snackTokens) {
    // Over-engineered: computing reserved tokens when the test doesn't check it
    int cost = item.drinkType() == DrinkType.NORMAL_ALCOHOLIC ? 1 : 0;
    int reservedTokens = drinkTokens - cost;
    
    if (reservedTokens < 0) {
        throw new InsufficientTokensException();
    }
    
    return new Order(item);  // Test passes but with unnecessary logic
}
```

### ❌ WRONG: Creating a FestivalGoer model prematurely
```java
// WRONG - The test passes primitives, not a FestivalGoer object
public Order placeOrder(String festivalGoerId, OrderItem item, int drinkTokens, int snackTokens) {
    var festivalGoer = new FestivalGoer(festivalGoerId, drinkTokens, snackTokens);  // Over-design
    return new Order(festivalGoer, item);
}
```

### ❌ WRONG: Creating a repository that the test doesn't use
```java
// WRONG - The test never injects or calls a repository
public class PlaceOrderUseCase {
    private OrderRepository repo;  // Not needed; test doesn't use it
    
    public PlaceOrderUseCase(OrderRepository repo) {  // Forcing dependency
        this.repo = repo;
    }
    
    public Order placeOrder(String festivalGoerId, OrderItem item, int drinkTokens, int snackTokens) {
        var order = new Order(item);
        repo.save(order);  // Over-engineering; test doesn't check persistence
        return order;
    }
}
```

### ❌ WRONG: Adding multiple DrinkType enum values
```java
// WRONG - The test only uses NORMAL_ALCOHOLIC; REFACTOR will add others
public enum DrinkType {
    NON_ALCOHOLIC,      // Not tested yet
    NORMAL_ALCOHOLIC,   // Tested
    PREMIUM_ALCOHOLIC;  // Not tested yet
}
```

### ❌ WRONG: Adding multiple OrderStatus enum values
```java
// WRONG - The test only expects PENDING
public enum OrderStatus {
    PENDING,       // Tested
    ACKNOWLEDGED,  // Not tested yet
    READY,         // Not tested yet
    CANCELLED;     // Not tested yet
}
```

---

## Output: Structured Result (JSON)

After implementing and passing the test, provide output in this format:

```json
{
  "phase": "GREEN",
  "test_file": "domain/src/test/java/com/it/exalt/belair/domain/order/usecases/PlaceOrderUseCaseTest.java",
  "test_method": "placeOrder_shouldCreateOrderWithPendingStatus_whenPlacingOrderWithSingleNormalAlcoholicDrink",
  "test_status": "PASSING",
  "implementation_summary": {
    "classes_created": [],
    "classes_modified": [
      {
        "name": "OrderItem",
        "file": "domain/src/main/java/com/it/exalt/belair/domain/order/model/OrderItem.java",
        "changes": [
          "Implemented OrderItem.createDrinkItem() factory method",
          "Returns new OrderItem record instance"
        ]
      },
      {
        "name": "Order",
        "file": "domain/src/main/java/com/it/exalt/belair/domain/order/model/Order.java",
        "changes": [
          "Added constructor: Order(OrderItem item)",
          "Implemented getStatus() to return OrderStatus.PENDING",
          "Implemented getItemCount() to return 1",
          "Implemented getDrinkTokenCost() with minimal logic: return 1 if NORMAL_ALCOHOLIC, 0 otherwise"
        ]
      },
      {
        "name": "PlaceOrderUseCase",
        "file": "domain/src/main/java/com/it/exalt/belair/domain/order/usecases/PlaceOrderUseCase.java",
        "changes": [
          "Implemented placeOrder() to create and return new Order(item)"
        ]
      }
    ],
    "enums_modified": [],
    "interfaces_created": [],
    "fakes_created": [],
    "total_lines_added": 15,
    "total_lines_removed": 3
  },
  "assertions_passing": [
    "order is not null",
    "order.getStatus() == OrderStatus.PENDING",
    "order.getItemCount() == 1",
    "order.getDrinkTokenCost() == 1"
  ],
  "design_decisions": [
    "Hardcoded OrderStatus.PENDING in getStatus() — only PENDING status is tested",
    "Hardcoded itemCount return 1 — only one item is tested",
    "Minimal drink token cost logic — only normal alcoholic (1 token) is tested",
    "No token reservation, no persistence, no event publishing — test doesn't require these"
  ],
  "next_steps": [
    "Run GREEN test: ./gradlew domain:test --tests '*PlaceOrderUseCaseTest#placeOrder_shouldCreateOrderWithPendingStatus*'",
    "Verify all assertions pass",
    "Move to REFACTOR phase when ready to extract and clean code"
  ]
}
```

---

## Execution Checklist

- [ ] Read the RED test method completely
- [ ] Understand all test inputs and expected outputs
- [ ] Implement minimal code to make test pass (no over-engineering)
- [ ] Run the test: it MUST pass
- [ ] Verify no other tests were modified
- [ ] Produce the JSON output
- [ ] Confirm all design decisions are documented

