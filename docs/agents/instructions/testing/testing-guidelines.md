# Testing Guidelines - Common Conventions

## Naming Conventions

**Class naming:**
- Unit tests: `[ClassUnderTest]Test` (e.g., `OrderTest`)
- Integration tests: `[ClassUnderTest]IntegrationTest` (e.g., `OrderRepositoryIntegrationTest`)

**Method naming:**
- **ALWAYS** apply this naming convention
- Pattern: `[methodName]_should[ExpectedBehavior]_when[Scenario]()`
- Example: 
    - Correct : `create_shouldPersistOrder_whenBasketIsValid()`
    - Incorrect : `shouldCreateOrderWithPendingStatusWhenPlacingSimpleOrder()`

**Variable naming:**
- `sut` = System Under Test (the object being tested)

---

## Test Structure: Given-When-Then

All tests follow this BDD structure with explicit section markers:

```java
class OrderServiceTest {
    private OrderService sut;  // System Under Test: the class being tested
    private FakeOrderRepository orderRepository;
    
    @BeforeEach
    void setUp() {
        orderRepository = new FakeOrderRepository();
        sut = new OrderService(orderRepository);
    }
    
    @Test
    void createOrder_shouldPersistOrder_whenBasketIsValid() {
        // GIVEN: prepare test data and pre-conditions
        var basket = new Basket("cust-1", List.of(new Item("item-1", 2)));
        
        // WHEN: call the method being tested on sut
        sut.createOrder(basket);
        
        // THEN: assert the expected results
        assertThat(orderRepository.findAll()).hasSize(1);
    }
}
```

**Key:** `sut` is the **class being tested** (OrderService), not objects that transit through it.

---

## Core Principles

- **Dependency Inversion**: Tests depend only on abstractions (interfaces/ports), never concrete implementations.
- **Fast & Isolated**: Tests are independent, deterministic, and run in milliseconds (domain) to seconds (integration).
- **Assertions**: Use AssertJ with specific, clear assertions—`assertThat(result).isEqualTo(expected)`.
- **No Over-Mocking**: Use lightweight in-process fakes for domain tests. Reserve Mockito for infrastructure only.

---

## Test Specific Conventions

When writing tests for each layer:

- **Testing domain business logic?** → See [domain-testing-guidelines.md](domain-testing-guidelines.md)
- **Testing REST API endpoints?** → See [application-testing-guidelines.md](application-testing-guidelines.md)
- **Testing persistence adapters?** → See [infrastructure-testing-guidelines.md](infrastructure-testing-guidelines.md)
