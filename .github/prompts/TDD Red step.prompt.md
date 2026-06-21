---
agent: agent
name: TDD Red step
description: This prompt is used to implement one test scenario that fails in a TDD workflow for an AI agent
argument-hint: Implement the following test scenario in a TDD workflow for an AI agent: {scenario_description}
tools: ['execute/getTerminalOutput', 'execute/runInTerminal', 'read/problems', 'read/readFile', 'read/terminalSelection', 'read/terminalLastCommand', 'edit/createDirectory', 'edit/createFile', 'edit/editFiles', 'search', 'upstash/context7/*', 'todo']
model: Claude Haiku 4.5 (copilot)
---

# Red TDD step prompt

## Instructions

**ABSOLUTE RULE: Do the bare minimum to make the test code compile. Period. Anticipation is poisoning.**

1. **Extract and understand the scenario:**
   - If the scenario is a reference (e.g., issue #123), retrieve the exact scenario text.
   - If provided directly, parse it as-is.
   - **Your job is to make THIS scenario fail, not to predict future scenarios.**

2. **Locate or create the test file:**
   - Check if a test file exists for this use case.
   - If yes: append the new test method to the existing file.
   - If no: create a new test file in `src/test/java` following module conventions (domain/application/infrastructure).

3. **Write the test first:**
   - Write ONLY the test code that directly reflects the scenario's Given-When-Then.
   - Follow [testing-guidelines.md](../../docs/agents/instructions/testing/testing-guidelines.md) for structure and naming.
   - For your specific layer, consult: [domain-testing-guidelines.md](../../docs/agents/instructions/testing/domain-testing-guidelines.md), [application-testing-guidelines.md](../../docs/agents/instructions/testing/application-testing-guidelines.md), or [infrastructure-testing-guidelines.md](../../docs/agents/instructions/testing/infrastructure-testing-guidelines.md).
   - **The test will fail because production code is missing.**

4. **Create ONLY structural skeletons in `src/main/java` to fix compilation errors.**
   - For each compilation error, create the **minimum required class/method/enum**.
   - **CRITICAL: Never create an interface, port, or abstract class unless the test directly imports it.**
   - **DO NOT CREATE:**
     - Repository interfaces unless the test explicitly instantiates or imports them.
     - EventPublisher ports unless the test calls `publish()` on one.
     - Enums with more values than what the test references.
     - Getters/setters that the test does not call.
     - Builder patterns, mappers, or utility classes.
   - **Skeleton syntax is ruthlessly minimal:**
     - **Classes**: Constructor(s) only. Method bodies: `throw new UnsupportedOperationException("Not implemented yet");`
     - **Methods**: Return `null` for objects, `0` for primitives, or throw exception (preferred).
     - **Enums**: List **ONLY** the exact enum values used in the test. If the test references only `PENDING`, the enum has only `PENDING`. Do not add `ACKNOWLEDGED`, `READY`, or `CANCELLED`.
     - **No conditionals, loops, assignments, or any logic whatsoever.**

5. **Fakes are test-only helper classes:**
   - Create Fakes in `src/test/java` ONLY if needed for the test to run.
   - A Fake is a **standalone class**, never implementing production interfaces that haven't been created yet.
   - Example: if you need to simulate storing data, write a simple `FakeStorage` class with a `List<>`. Do not create a `StoragePort` interface in `src/main/java`.
   - Store Fakes in `src/test/java` in a dedicated `fake/` or `helper/` package.

6. **Run the test to confirm failure:**
   - Execute the test. It **MUST fail** (either `UnsupportedOperationException` or assertion failure).
   - A test that passes or doesn't execute is an absolute failure of RED.

## Requirements

**Zero tolerance for anticipation. These rules are non-negotiable.**

- **ABSOLUTE: No production interfaces, ports, or abstract classes in `src/main/java` unless the test directly imports them.**
  - If you write `OrderRepository` in `src/main/java` but the test never imports it, you failed RED.
  - If you need to pass something to the UseCase constructor, check first: can the test pass a simple mock/Fake from `src/test/java` instead? If yes, do that.

- **ABSOLUTE: Enums contain ONLY the values referenced in the test.**
  - If the test asserts `status == PENDING`, the enum has only `PENDING`.
  - If a scenario future might need `ACKNOWLEDGED`, ignore it. That's GREEN's job.
  - **Violation example**: Creating `OrderStatus { PENDING, ACKNOWLEDGED, READY, CANCELLED }` when the test only uses `PENDING` is a failure.

- **ABSOLUTE: Classes contain ONLY methods called by the test.**
  - Do not add getters, setters, or helper methods the test doesn't invoke.
  - Do not add fields unless the test reads or writes them.
  - **Violation example**: Adding `getTotalCost()` or `reserve()` methods when the test doesn't call them is over-design.

- **ABSOLUTE: No nested or intermediate structures.**
  - Do not create separate Mapper classes, DTO classes, or domain-event classes unless the test instantiates or references them.
  - Do not create builder patterns, factory classes, or utility wrappers.
  - **Violation example**: Creating `OrderPlacedEvent` or `PlaceOrderRequest` or `PlaceOrderResponse` when the test uses simple parameters is over-engineering.

- **ABSOLUTE: Follow the module testing guidelines.**
  - Domain tests: [domain-testing-guidelines.md](../../docs/agents/instructions/testing/domain-testing-guidelines.md) (Fakes, no Spring)
  - Application tests: [application-testing-guidelines.md](../../docs/agents/instructions/testing/application-testing-guidelines.md) (@WebMvcTest, MockMvc, mock UseCases)
  - Infrastructure tests: [infrastructure-testing-guidelines.md](../../docs/agents/instructions/testing/infrastructure-testing-guidelines.md) (Testcontainers, adapters, mappers)
  - Use the test pattern, naming conventions, and assertion styles defined in each guideline.

- **ABSOLUTE: The test MUST fail when executed.**
  - Compilation success ≠ RED success. The test must execute and fail (throw exception or assertion error).
  - If the test passes, you violated RED by implementing production logic.

## Examples

### Domain test example: Place Order scenario (ultra-minimal RED)

**Input scenario:**
```
Scenario: Place order with single normal alcoholic drink
  Given a festival goer with ID "fgv-001"
  And the festival goer has 6 drink tokens and 9 snack tokens
  When placing an order with 1 normal alcoholic drink
  Then an Order is created with status PENDING
  And the order contains 1 drink item
  And the total drink token cost is 1
  And the festival goer's drink tokens are reserved (1 reserved, 5 available)
```

**Test file** `domain/src/test/java/com/it/exalt/belair/domain/order/usecases/PlaceOrderUseCaseTest.java`:

All code (enums, classes, use case) as inner classes inside test:

```java
class PlaceOrderUseCaseTest {
    
    @Test
    void placeOrder_shouldCreateOrderWithPendingStatus_whenPlacingOrderWithSingleNormalAlcoholicDrink() {
        // GIVEN a festival goer with ID "fgv-001"
        String festivalGoerId = "fgv-001";
        // And the festival goer has 6 drink tokens and 9 snack tokens
        var balance = new FestivalGoerBalance(6, 9);
        
        // WHEN placing an order with 1 normal alcoholic drink
        var sut = new PlaceOrderUseCase();
        var item = OrderItem.createDrinkItem(DrinkType.NORMAL_ALCOHOLIC, 1);
        Order order = sut.placeOrder(festivalGoerId, item, balance);
        
        // THEN an Order is created with status PENDING
        assertThat(order).isNotNull();
        assertThat(order.getStatus()).isEqualTo(OrderStatus.PENDING);
        // And the order contains 1 drink item
        assertThat(order.getItemCount()).isEqualTo(1);
        // And the total drink token cost is 1
        assertThat(order.getDrinkTokenCost()).isEqualTo(1);
    }
    
    // ============ INNER CLASSES: ALL PRODUCTION CODE BELOW ============
    
    enum DrinkType {
        NORMAL_ALCOHOLIC  // Only this value referenced in test
    }
    
    enum OrderStatus {
        PENDING  // Only this value referenced in test
    }
    
    record OrderItem(DrinkType drinkType, int quantity) {
        static OrderItem createDrinkItem(DrinkType type, int quantity) {
            throw new UnsupportedOperationException("Not implemented yet");
        }
    }
    
    static class Order {
        OrderStatus getStatus() {
            throw new UnsupportedOperationException("Not implemented yet");
        }
        
        int getItemCount() {
            throw new UnsupportedOperationException("Not implemented yet");
        }
        
        int getDrinkTokenCost() {
            throw new UnsupportedOperationException("Not implemented yet");
        }
    }
    
    record FestivalGoerBalance(int drinkTokens, int snackTokens) {}
    
    static class PlaceOrderUseCase {
        Order placeOrder(String festivalGoerId, OrderItem item, FestivalGoerBalance balance) {
            throw new UnsupportedOperationException("Not implemented yet");
        }
    }
}
```

**Key points:**
- All code (enums, records, classes, use case) in inner classes inside test
- Enums contain ONLY values referenced in test (`NORMAL_ALCOHOLIC`, `PENDING`)
- Classes contain ONLY methods called by test
- No files created in `src/main/java` — everything in test ✓
- Test compiles ✓
- Test runs and fails: `UnsupportedOperationException: Not implemented yet` ✓
- All code extraction happens in REFACTOR phase ✓

**What is NOT created in RED:**
- ❌ `OrderRepository` interface (test doesn't import it)
- ❌ `TokenBalanceRepository` interface (test doesn't need persistence yet)
- ❌ `EventPublisherPort` interface (test doesn't publish events)
- ❌ `OrderPlacedEvent` class (test doesn't assert events)
- ❌ Mapper classes (no mapping needed in RED)
- ❌ Builder patterns, factory utilities, or DSLs

---

### Application Layer Example: POST /api/orders Endpoint (RED)

**Test file** `application/src/test/java/com/it/exalt/belair/application/order/rest/PlaceOrderControllerTest.java`:

Everything in inner classes inside the test (NO files in src/main/java):

```java
@WebMvcTest
class PlaceOrderControllerTest {
    @Autowired private MockMvc mockMvc;
    @MockBean private PlaceOrderUseCase placeOrderUseCase;
    
    private static final String ORDERS_ENDPOINT = "/api/orders";
    
    @Test
    void post_shouldReturn201_whenOrderIsValid() throws Exception {
        // GIVEN a valid order request
        var request = """{"festivalGoerId": "fgv-001", "items": [], "drinkTokens": 6, "snackTokens": 9}""";
        var mockOrder = new Order("order-1", "fgv-001");
        given(placeOrderUseCase.placeOrder(any())).willReturn(mockOrder);
        
        // WHEN posting the request
        // THEN expect 201 Created
        mockMvc.perform(post(ORDERS_ENDPOINT)
            .contentType(MediaType.APPLICATION_JSON)
            .content(request))
            .andExpect(status().isCreated());
    }
    
    // ============ INNER CLASSES: ALL PRODUCTION CODE BELOW ============
    
    @RestController
    @RequestMapping("/api/orders")
    static class PlaceOrderController {
        private final PlaceOrderUseCase placeOrderUseCase;
        
        PlaceOrderController(PlaceOrderUseCase placeOrderUseCase) {
            this.placeOrderUseCase = placeOrderUseCase;
        }
        
        @PostMapping
        ResponseEntity<PlaceOrderResponse> post(@RequestBody PlaceOrderRequest request) {
            throw new UnsupportedOperationException("Not implemented yet");
        }
    }
    
    record PlaceOrderRequest(
        String festivalGoerId,
        List<?> items,
        int drinkTokens,
        int snackTokens
    ) {}
    
    record PlaceOrderResponse(String orderId, String status) {}
    
    // Mock Order from Domain (inner class for test)
    static class Order {
        private String id;
        private String festivalGoerId;
        
        Order(String id, String festivalGoerId) {
            this.id = id;
            this.festivalGoerId = festivalGoerId;
        }
        
        String id() { return id; }
        String festivalGoerId() { return festivalGoerId; }
    }
}
```

**What is created in src/main/java:**
- ❌ **NOTHING** — all code in test inner classes
- The test compiles ✓
- The test runs and fails: `UnsupportedOperationException: Not implemented yet` ✓

---

### Infrastructure Layer Example: Order Persistence Adapter (RED)

**Test file** `infrastructure/src/test/java/com/it/exalt/belair/infrastructure/order/persistence/OrderRepositoryAdapterIntegrationTest.java`:

Everything in inner classes inside the test (NO files in src/main/java):

```java
@Testcontainers
class OrderRepositoryAdapterIntegrationTest {
    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15");
    
    @Test
    void save_shouldPersistOrderAndRetrieveItCorrectly() {
        // GIVEN a domain order (mock)
        var order = new Order("order-1", "fgv-001");
        
        // WHEN saving via adapter (inner class)
        var adapter = new OrderRepositoryAdapter();
        adapter.save(order);
        
        // THEN it should be retrievable
        var retrieved = adapter.find("order-1");
        assertThat(retrieved).isPresent();
    }
    
    // ============ INNER CLASSES: ALL PRODUCTION CODE BELOW ============
    
    static class OrderRepositoryAdapter {
        private final JpaOrderRepository jpaRepository = null;  // Minimal: not initialized
        
        void save(Order order) {
            throw new UnsupportedOperationException("Not implemented yet");
        }
        
        Optional<Order> find(String orderId) {
            throw new UnsupportedOperationException("Not implemented yet");
        }
    }
    
    @Entity
    @Table(name = "orders")
    static class OrderJpaEntity {
        @Id String id;
        @Column String festivalGoerId;
        
        // Minimal: no constructor, no getters
    }
    
    static class OrderMapper {
        OrderJpaEntity toEntity(Order domain) {
            throw new UnsupportedOperationException("Not implemented yet");
        }
        
        Order toDomain(OrderJpaEntity entity) {
            throw new UnsupportedOperationException("Not implemented yet");
        }
    }
    
    interface JpaOrderRepository {
        void save(OrderJpaEntity entity);
        Optional<OrderJpaEntity> findById(String id);
    }
    
    // Mock Order from Domain (inner class for test)
    static class Order {
        private String id;
        private String festivalGoerId;
        
        Order(String id, String festivalGoerId) {
            this.id = id;
            this.festivalGoerId = festivalGoerId;
        }
        
        String id() { return id; }
        String festivalGoerId() { return festivalGoerId; }
    }
}
```

**What is created in src/main/java:**
- ❌ **NOTHING** — all code in test inner classes
- The test compiles ✓
- The test runs and fails: `UnsupportedOperationException: Not implemented yet` ✓